package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.debugger.engine.JVMNameUtil
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.ClassUtil
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile.CompilableSourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates.Companion.fromAbsolutePaths
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates.Companion.fromRelativePaths
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates.RelativeClassFileCandidates
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.ClassFilePreparationTask
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.base.psi.classIdIfNonLocal
import org.jetbrains.kotlin.idea.debugger.core.ClassNameProvider
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KotlinDeclarationNavigationPolicy
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

@Service(Service.Level.PROJECT)
internal class ClassFilesFinderService(private val project: Project) {
  // -- Properties ---------------------------------------------------------- //

  private val projectFileIndex by lazy { ProjectFileIndex.getInstance(project) }
  private val classNameProvider by lazy {
    ClassNameProvider(
      project,
      GlobalSearchScope.allScope(project),
      ClassNameProvider.Configuration.DEFAULT.copy(alwaysReturnLambdaParentClass = true),
    )
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun findByVirtualFiles(virtualFiles: List<VirtualFile>): Result {
    return virtualFiles
      .mapNotNull { virtualFile ->
        if (virtualFile.isDirectory) {
          return@mapNotNull null
        }

        if (virtualFile.isClassFile()) {
          return@mapNotNull Result.withClassFileToOpen(
            ClassFile(file = virtualFile, sourceFile = findSource(virtualFile))
          )
        }

        if (DumbService.isDumb(project)) {
          return@mapNotNull Result.withErrorDumbMode()
        }

        val psiFile =
          runReadAction { PsiManager.getInstance(project).findFile(virtualFile) }
            ?: return@mapNotNull Result.withErrorNoContainingFileForElement()
        findByPsiFiles(listOf(psiFile))
      }
      .reduce()
  }

  fun findByPsiFiles(psiFiles: List<PsiFile>): Result {
    return psiFiles
      .map { psiFile ->
        // Continue to work with the `workingFile` instead of the `psiFile` to
        // avoid confusion.
        val workingFile =
          WorkingFile.fromPsiElement(psiFile, psiFile)
            ?: return@map Result.withErrorNoContainingFileForElement()

        if (workingFile.virtualFile.isClassFile()) {
          return@map Result.withClassFileToOpen(
            ClassFile(
              file = workingFile.virtualFile,
              sourceFile = findSource(workingFile.virtualFile),
            )
          )
        }

        // In dumb mode, the `workingFile.psiFile` may not be a `PsiJavaFile`
        // or `PsiClassOwner`, which may lead to incorrect results.
        if (DumbService.isDumb(project)) {
          return@map Result.withErrorDumbMode()
        }

        if (workingFile.psiFile is PsiJavaFile) {
          // package-info.java
          if (workingFile.psiFile.name == "package-info.java") {
            // Only exists in sources
            return@map Result.withErrorNotProcessableFile(workingFile.psiFile)
          }

          // module-info.java
          val moduleDeclaration = runReadAction { workingFile.psiFile.moduleDeclaration }
          if (moduleDeclaration != null) {
            return@map findByPsiElements(moduleDeclaration, workingFile)
          }
        }

        val psiClasses =
          if (workingFile.psiFile is PsiClassOwner) runReadAction { workingFile.psiFile.classes }
          else emptyArray<PsiClass>()
        if (psiClasses.isNotEmpty()) {
          return@map psiClasses.map { findClassFilesFromPsiClass(it, workingFile) }.reduce()
        }

        // Kotlin file without a class
        if (workingFile.psiFile is KtFile) {
          val javaFileFacadeFqName = runReadAction { workingFile.psiFile.javaFileFacadeFqName }
          return@map workingFile.toResult(
            fromRelativePaths(javaFileFacadeFqName.asString().toRelativeClassFilePath())
          )
        }

        return@map Result.withErrorNotProcessableFile(workingFile.psiFile)
      }
      .reduce()
  }

  fun findByPsiElements(psiElementToPsiElementOriginPsiFile: Map<PsiElement, PsiFile?>): Result =
    psiElementToPsiElementOriginPsiFile
      .map { (psiElement, psiElementOriginPsiFile) ->
        WorkingFile.fromPsiElement(psiElement, psiElementOriginPsiFile)?.let { workingFile ->
          findByPsiElements(psiElement, workingFile)
        } ?: Result.withErrorNoContainingFileForElement()
      }
      .reduce()

  fun findByClassFiles(classFiles: List<ClassFile>): Result {
    return classFiles
      .map { classFile ->
        if (classFile.sourceFile is CompilableSourceFile) {
          val compilerOutputClassFilePath = fromAbsolutePaths(classFile.file.toNioPath())
          Result.withClassFileToPrepare(
            ClassFilePreparationTask(compilerOutputClassFilePath, classFile.sourceFile)
          )
        } else {
          Result.withClassFileToOpen(classFile)
        }
      }
      .reduce()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun findByPsiElements(psiElement: PsiElement, workingFile: WorkingFile): Result {
    if (DumbService.isDumb(project)) {
      return if (workingFile.virtualFile.isClassFile()) {
        // Fallback to the original file
        // Advantage:
        // If the user opens the .class file in the out directory from the
        // project view, IntelliJ would give us a PsiClass element. But if this
        // is happening during indexing, it would not be possible to open the
        // file although the indexing has no effect on the compiled file.
        // Disadvantage:
        // If the user tries to open an inner class of a decompiled .class file
        // during indexing, we would now open the outermost class file.
        Result.withClassFileToOpen(ClassFile(workingFile.virtualFile))
      } else {
        Result.withErrorDumbMode()
      }
    }

    if (psiElement is PsiFile) {
      return findByPsiFiles(listOf(psiElement))
    }

    // If the user opens a class file from a library and IntelliJ shows the
    // corresponding source file from the source JAR (not the decompiled one),
    // the `psiElement.originalElement?.containingFile` should point to that
    // source file but not the original class file. Since we can't compile
    // this file, we have to get the original class file.
    if (isInLibrary(workingFile.virtualFile)) {
      val libraryResult: Result =
        findClassFilesIfOriginalElementIsInLibrary(psiElement, workingFile, failWithError = false)
      if (libraryResult.isNotEmpty()) {
        return libraryResult
      }
    }

    // Module descriptor
    val containingModule = runReadAction { psiElement.getParentOfType<PsiJavaModule>(false) }
    if (containingModule != null) {
      // A module descriptor does not have nested classes. Therefore, we can
      // directly consume the containing file.
      return workingFile.toResult(fromRelativePaths(Path.of("module-info.class")))
    }

    // Element in a class
    return findClassFilesFromContainingClass(psiElement, workingFile)
  }

  private fun findClassFilesIfOriginalElementIsInLibrary(
    psiElement: PsiElement,
    workingFile: WorkingFile,
    alternativeRelativeClassFilePathCandidates: ClassFileCandidates? = null,
    failWithError: Boolean = true,
  ): Result {
    assert(isInLibrary(workingFile.virtualFile))

    // The relative class file path may be a nested class in the `workingFile.virtualFile`
    val relativeClassFilePathCandidates =
      psiElement.findRelativeClassFilePathFromContainingClass()
        ?: alternativeRelativeClassFilePathCandidates
    if (relativeClassFilePathCandidates != null) {
      // If a file of a library gets opened, the `originalElementContainingFile`
      // will be a source file. One of the elements in that file, probably the
      // first class element, will be linked to the `originalElement` of the
      // class file.
      val searchDirectory =
        runReadAction { workingFile.psiFile.children }
          .firstNotNullOfOrNull {
            val containingVirtualFile = runReadAction {
              it.originalElement.containingFile.virtualFile
            }
            if (containingVirtualFile?.isClassFile() == true) containingVirtualFile else null
          }
          ?.parent
      if (searchDirectory != null) {
        val classVirtualFile =
          relativeClassFilePathCandidates.allPaths().firstNotNullOfOrNull {
            searchDirectory.findFile(it.fileName.toString())
          }
        if (classVirtualFile != null) {
          val sourceFile =
            findSource(
              if (!workingFile.virtualFile.isClassFile()) workingFile.virtualFile
              else classVirtualFile
            )
          return findByClassFiles(
            listOf(ClassFile(file = classVirtualFile, sourceFile = sourceFile))
          )
        } else if (failWithError) {
          relativeClassFilePathCandidates.formatNotFoundError(
            "cannot be found in directory '${searchDirectory.path}'.",
            project,
          )
        }
      } else if (failWithError) {
        relativeClassFilePathCandidates.formatNotFoundError("cannot be found.", project)
      }
    }

    return if (failWithError && psiElement is PsiFile) {
      Result.withErrorNotProcessableFile(psiElement)
    } else if (failWithError) {
      Result.withErrorNoContainingFileForElement()
    } else {
      Result.empty()
    }
  }

  private fun isInLibrary(virtualFile: VirtualFile) = runReadAction {
    projectFileIndex.isInLibrary(virtualFile)
  }

  private fun PsiClass.getFqClassName(): String? = runReadAction {
    JVMNameUtil.getClassVMName(this)
  }

  private fun PsiElement.findRelativeClassFilePathFromContainingClass():
    RelativeClassFileCandidates? {
    val containingFile = runReadAction { containingFile }
    return if (containingFile is KtFile) {
      val relativeClassFilePaths =
        findContainingClassNameCandidates().map { classId ->
          val packagePath =
            Paths.get(
              "",
              *classId.packageFqName
                .pathSegments()
                .map { it.asStringStripSpecialMarkers() }
                .toTypedArray(),
            )
          val fileName =
            classId.relativeClassName.pathSegments().joinToString(
              separator = "$",
              postfix = ".class",
            ) {
              it.asStringStripSpecialMarkers()
            }
          packagePath.resolve(fileName)
        }
      if (relativeClassFilePaths.isNotEmpty())
        fromRelativePaths(*relativeClassFilePaths.toTypedArray())
      else null
    } else if (containingFile.name.substringBefore(".") == "module-info") {
      fromRelativePaths(Path.of("module-info.class"))
    } else {
      runReadAction { getParentOfType<PsiClass>(false)?.getFqClassName() }
        ?.toRelativeClassFilePath()
        ?.let { fromRelativePaths(it) }
    }
  }

  /**
   * If we are in a local class, we can't determine the class file name based on the PSI data model,
   * since it's up to the compiler on how to name the class (e.g., `Foo$method$1`). For this case,
   * we will use the next non-local parent class or object.
   */
  private fun findClassFilesFromContainingClass(
    psiElement: PsiElement,
    workingFile: WorkingFile,
  ): Result {
    // Kotlin code
    if (workingFile.psiFile is KtFile) {
      val relativeClassFilePathCandidates =
        psiElement.findRelativeClassFilePathFromContainingClass()
      if (relativeClassFilePathCandidates != null) {
        return workingFile.toResult(relativeClassFilePathCandidates)
      }

      // Fallback to the containing file
      return if (workingFile.isClassFile) {
        // The containing file is a class file and therefore has an unambiguous
        // JVM class.
        val sourceFile = findSource(workingFile.virtualFile)
        Result.withClassFileToOpen(ClassFile(workingFile.virtualFile, sourceFile))
      } else {
        // The containing file is a source file. Therefore, we do not have an
        // unambiguous JVM class based on the selected element.
        findClassFilesFromPsiFile(
          runReadAction { psiElement.containingFile.originalFile },
          workingFile,
        )
      }
    }

    // Continue with generic PSI module

    // A light method is a synthetic method in an inner enum or record.
    val containingClass: PsiClass? = runReadAction {
      var containingClass = psiElement.getParentOfType<LightMethod>(false)?.containingClass

      if (containingClass == null) {
        containingClass = psiElement.getParentOfType<PsiClass>(false)
      }

      while (containingClass is PsiTypeParameter) {
        containingClass = containingClass.getParentOfType<PsiClass>(false)
      }

      return@runReadAction containingClass
    }

    if (containingClass == null) {
      return findClassFilesFromPsiFile(workingFile.psiFile, workingFile)
    }

    return findClassFilesFromPsiClass(containingClass, workingFile)
  }

  private fun WorkingFile.toResult(
    relativeClassFilePathCandidates: RelativeClassFileCandidates
  ): Result {
    return if (isClassFile) {
      relativeClassFilePathCandidates.allPaths().firstNotNullOfOrNull {
        relativeClassFilePathCandidate ->
        virtualFile.parent
          .findFileByRelativePath(relativeClassFilePathCandidate.fileName.toString())
          ?.let { classVirtualFile ->
            Result.withClassFileToOpen(
              ClassFile(file = classVirtualFile, sourceFile = findSource(virtualFile))
            )
          }
      }
        ?: Result.withError(
          relativeClassFilePathCandidates.formatNotFoundError(
            "cannot be found in directory '${virtualFile.parent.path}'.",
            project,
          )
        )
    } else if (isInLibrary(virtualFile)) {
      findClassFilesIfOriginalElementIsInLibrary(
        psiFile,
        this,
        relativeClassFilePathCandidates,
        failWithError = true,
      )
    } else {
      findClassFileInCompilerOutputDirOfSourceFile(relativeClassFilePathCandidates, virtualFile)
    }
  }

  private fun findSource(virtualFile: VirtualFile): SourceFile.NonCompilableSourceFile? {
    if (!virtualFile.isClassFile()) {
      return SourceFile.NonCompilableSourceFile(virtualFile)
    }

    var classVirtualFileToUse = virtualFile
    if (virtualFile.name.contains("$")) {
      // The `FileSwapper`s in the next step will not find the source file for
      // a nested class file. For this case, we have to work with the containing
      // class file.
      classVirtualFileToUse =
        virtualFile.parent?.findFile("${virtualFile.name.substringBefore('$')}.class")
          ?: virtualFile
    }

    val psiFile = runReadAction { PsiManager.getInstance(project).findFile(classVirtualFileToUse) }
    return if (psiFile is PsiCompiledFile && psiFile is PsiClassOwner) {
      // See `JavaEditorFileSwapper#findSourceFile`
      // Only `ClsClassImpl` have a `sourceMirrorClass` which will lead to the
      // real source file (if it exists). All other `Cls*` instance, for example
      // a module descriptor just have a `mirror` that is not bound to the real
      // source file, only the virtual decompiled PSI file.
      runReadAction { (psiFile as PsiClassOwner).classes }
        .takeIf { classes -> classes.isNotEmpty() && classes[0] is ClsClassImpl }
        ?.let { classes ->
          runReadAction {
            (classes[0] as ClsClassImpl).sourceMirrorClass?.containingFile?.virtualFile
          }
        }
        ?.let { SourceFile.NonCompilableSourceFile(it) }
    } else if (psiFile is KtDecompiledFile) {
      // See `KotlinEditorFileSwapper#getSourcesLocation`
      runReadAction { psiFile.declarations }
        .firstOrNull()
        ?.let { declaration ->
          val kotlinDeclarationNavigationPolicy = serviceOrNull<KotlinDeclarationNavigationPolicy>()
          runReadAction { kotlinDeclarationNavigationPolicy?.getNavigationElement(declaration) }
            ?.takeIf { it != declaration }
            ?.containingFile
            ?.takeIf { it.isValid }
            ?.virtualFile
            ?.let { SourceFile.NonCompilableSourceFile(it) }
        }
    } else {
      null
    }
  }

  private fun findClassFilesFromPsiFile(psiFile: PsiFile, workingFile: WorkingFile): Result {
    // We may reach this point if the selected element is, for example, an
    // input statement. So we are outside a class.
    if (psiFile !is PsiClassOwner) {
      return Result.withErrorNotProcessableFile(psiFile)
    }

    return runReadAction { psiFile.classes }
      .map { findClassFilesFromPsiClass(it, workingFile) }
      .reduce()
  }

  private fun findClassFilesFromPsiClass(psiClass: PsiClass, workingFile: WorkingFile): Result {
    val fqClassName =
      psiClass.getFqClassName() ?: return Result.withError("Unable to determine class name.")

    // If the `PsiClass` is an inner class in a class file, the `containingFile`
    // would return the file of the outermost class file.
    var potentialClassFile: VirtualFile? = runReadAction {
      psiClass.originalElement?.containingFile?.virtualFile
    }
    val simpleClassName = ClassUtil.extractClassName(fqClassName)
    potentialClassFile = potentialClassFile?.parent?.findFile("$simpleClassName.class")
    if (potentialClassFile != null && potentialClassFile.isClassFile()) {
      val sourceFile = findSource(workingFile.virtualFile)
      return Result.withClassFileToOpen(
        ClassFile(file = potentialClassFile, sourceFile = sourceFile)
      )
    }

    return workingFile.toResult(fromRelativePaths(fqClassName.toRelativeClassFilePath()))
  }

  private fun String.toRelativeClassFilePath(): Path =
    FileSystems.getDefault().getPath("${this.replace('.', '/')}.class")

  private fun findClassFileInCompilerOutputDirOfSourceFile(
    relativeClassFilePathCandidates: RelativeClassFileCandidates,
    sourceFilePath: VirtualFile,
  ): Result {
    val module = runReadAction { projectFileIndex.getModuleForFile(sourceFilePath) }
    if (module != null) {
      val sourceFile = CompilableSourceFile(sourceFilePath, module)
      val compilerOutputClassFilePaths =
        relativeClassFilePathCandidates.allPaths().mapNotNull {
          determineFullClassFilePathInCompilerOutputOfSourceFile(it, sourceFile)
        }
      if (compilerOutputClassFilePaths.isNotEmpty()) {
        return Result.withClassFileToPrepare(
          ClassFilePreparationTask(
            compilerOutputClassFileCandidates =
              fromAbsolutePaths(*compilerOutputClassFilePaths.toTypedArray()),
            sourceFile = sourceFile,
          )
        )
      }
    }

    return if (DumbService.isDumb(project)) {
      Result.withErrorDumbMode()
    } else {
      Result.withError(
        "Couldn't determine the expected class file path for source file '${sourceFilePath.name}'.\n" +
          "Try to recompile the project or, if it's a non-project Java source file, compile the file " +
          "separately and open the resulting class file directly."
      )
    }
  }

  private fun PsiElement.findContainingClassNameCandidates(): List<ClassId> = runReadAction {
    var parentKtClassOrObject: KtClassOrObject? =
      getParentOfType<KtClassOrObject>(false)
        ?:
        // If `this` is an element outside a class or object, the `classNameProvider`
        // will do a fallback and uses the file name as a "*Kt" class name. But this
        // is not a desired behaviour at this point.
        return@runReadAction emptyList()

    // `KtEnumEntry` is a value in an enum and is also a `KtClass` but it can't
    // be reference as a standalone class file. Therefore, we use the actual
    // enum as a parent class. Note the change in the `strict` parameter.
    val elementToUse = this.getParentOfType<KtEnumEntry>(false)?.parent ?: this
    val candidatesForElement = classNameProvider.getCandidatesForElement(elementToUse)
    if (candidatesForElement.isNotEmpty()) {
      return@runReadAction candidatesForElement.map { ClassId.topLevel(FqName(it)) }
    }

    // This is a simple fall back logic which will find a parent
    // non-local/anonymous class
    if (parentKtClassOrObject is KtEnumEntry) {
      parentKtClassOrObject = parentKtClassOrObject.getParentOfType<KtClassOrObject>(true)
    }

    if (parentKtClassOrObject != null && parentKtClassOrObject.classIdIfNonLocal == null) {
      return@runReadAction parentKtClassOrObject.parent.findContainingClassNameCandidates()
    }

    return@runReadAction parentKtClassOrObject?.classIdIfNonLocal?.let { listOf(it) } ?: emptyList()
  }

  private fun determineFullClassFilePathInCompilerOutputOfSourceFile(
    relativeClassFilePath: Path,
    sourceFile: CompilableSourceFile,
  ): Path? {
    val compiler = CompilerModuleExtension.getInstance(sourceFile.module) ?: return null
    val isTest = runReadAction { projectFileIndex.isInTestSourceContent(sourceFile.file) }
    // Don't use 'compilerOutputPath' here because if there is a compiler output
    // path but nothing was compiled yet (and therefore the directory does not
    // exist), it returns null.
    val classRoot =
      (if (isTest) compiler.compilerOutputForTestsPointer else compiler.compilerOutputPointer)
        ?: return null
    return Paths.get(classRoot.presentableUrl).resolve(relativeClassFilePath)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class WorkingFile(
    val psiFile: PsiFile,
    val virtualFile: VirtualFile,
    val isClassFile: Boolean,
  ) {

    companion object {

      fun fromPsiElement(psiElement: PsiElement, psiElementOriginPsiFile: PsiFile?): WorkingFile? {
        val psiFile =
          runReadAction { psiElement.originalElement?.containingFile }
            ?: psiElementOriginPsiFile
            ?: return null
        val virtualFile = psiFile.originalFile.virtualFile
        val isClassFile = virtualFile.isClassFile()
        return WorkingFile(psiFile, virtualFile, isClassFile)
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class Result(
    val classFilesToOpen: MutableList<ClassFile> = mutableListOf(),
    val classFilesToPrepare: MutableList<ClassFilePreparationTask> = mutableListOf(),
    val errors: MutableList<String> = mutableListOf(),
  ) {

    fun addResult(result: Result): Result {
      classFilesToOpen.addAll(result.classFilesToOpen)
      classFilesToPrepare.addAll(result.classFilesToPrepare)
      errors.addAll(result.errors)
      return this
    }

    fun isNotEmpty(): Boolean =
      classFilesToOpen.isNotEmpty() || classFilesToPrepare.isNotEmpty() || errors.isNotEmpty()

    companion object {

      fun withError(error: String) = Result(errors = mutableListOf(error))

      fun withErrorDumbMode() =
        Result(
          errors =
            mutableListOf("Analyse byte code is not available while indexes are being updated.")
        )

      fun withErrorNoContainingFileForElement() =
        Result(
          errors =
            mutableListOf("Unable to determine a source or class file for the selected element.")
        )

      fun withErrorNotProcessableFile(psiFile: PsiFile) =
        Result(
          errors =
            mutableListOf("File '${psiFile.name}' is not a processable source or class file.")
        )

      fun withClassFileToPrepare(classFileToPrepare: ClassFilePreparationTask) =
        Result(classFilesToPrepare = mutableListOf(classFileToPrepare))

      fun withClassFileToOpen(classFile: ClassFile) =
        Result(classFilesToOpen = mutableListOf(classFile))

      fun empty() = Result()
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    fun fileCanBeAnalysed(file: VirtualFile, project: Project): Boolean {
      if (file.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      if (file.isClassFile()) {
        return true
      }

      val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) }
      return psiFile != null && fileCanBeAnalysed(psiFile)
    }

    fun fileCanBeAnalysed(psiFile: PsiFile): Boolean {
      if (psiFile.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      return psiFile is PsiClassOwner
    }

    fun List<Result>.reduce() = this.reduceOrNull { a, b -> a.addResult(b) } ?: Result.empty()

    fun VirtualFile.isClassFile(): Boolean =
      FileTypeRegistry.getInstance().isFileOfType(this, JavaClassFileType.INSTANCE)
  }
}

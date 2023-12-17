package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiUtil
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile.CompilableSourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.ClassFilePreparationTask
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.base.psi.classIdIfNonLocal
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KotlinDeclarationNavigationPolicy
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.PROJECT)
internal class ClassFilesFinderService(private val project: Project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val projectFileIndex by lazy { ProjectFileIndex.getInstance(project) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun findByVirtualFiles(virtualFiles: List<VirtualFile>): Result {
    return virtualFiles.mapNotNull { virtualFile ->
      if (virtualFile.isDirectory) {
        return@mapNotNull null
      }

      if (virtualFile.isClassFile()) {
        return@mapNotNull Result.withClassFileToOpen(ClassFile(file = virtualFile, sourceFile = findSource(virtualFile)))
      }

      if (DumbService.isDumb(project)) {
        return@mapNotNull Result.withErrorDumbMode()
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(virtualFile)
        ?: return@mapNotNull Result.withErrorNoContainingFileForElement()
      findByPsiFiles(listOf(psiFile))
    }.reduce()
  }

  fun findByPsiFiles(psiFiles: List<PsiFile>): Result {
    return psiFiles.map { psiFile ->
      // Continue to work with the `workingFile` instead of the `psiFile` to
      // avoid confusion.
      val workingFile = WorkingFile.fromPsiElement(psiFile, psiFile)
        ?: return@map Result.withErrorNoContainingFileForElement()

      if (workingFile.virtualFile.isClassFile()) {
        return@map Result.withClassFileToOpen(
          ClassFile(
            file = workingFile.virtualFile,
            sourceFile = findSource(workingFile.virtualFile)
          )
        )
      }

      if (DumbService.isDumb(project)) {
        return@map Result.withErrorDumbMode()
      }

      if (workingFile.psiFile is PsiJavaFile) {
        // package-info.java
        if (workingFile.psiFile.name == "package-info.java") {
          // Only exists in sources
          return Result.empty()
        }

        // module-info.java
        val moduleDeclaration = workingFile.psiFile.moduleDeclaration
        if (moduleDeclaration != null) {
          return@map findByPsiElement(moduleDeclaration, workingFile)
        }
      }

      if (workingFile.psiFile is PsiClassOwner) {
        val psiClasses = workingFile.psiFile.classes
        if (psiClasses.isNotEmpty()) {
          return psiClasses
            .map {
              findClassFilesFromPsiClass(it, workingFile)
            }
            .reduceOrNull { a, b -> a.addResult(b) } ?: Result.empty()
        }
      }

      // Kotlin file without a class
      if (workingFile.psiFile is KtFile && workingFile.psiFile.classes.isEmpty()) {
        return workingFile.toResult(workingFile.psiFile.javaFileFacadeFqName.asString().toRelativeClassFilePath())
      }

      Result.withErrorNotProcessableFile(workingFile.psiFile)
    }.reduce()
  }

  fun findByPsiElement(psiElement: PsiElement, psiElementOriginPsiFile: PsiFile?): Result {
    val workingFile = WorkingFile.fromPsiElement(psiElement, psiElementOriginPsiFile)
      ?: return Result.withErrorNoContainingFileForElement()
    return findByPsiElement(psiElement, workingFile)
  }

  fun findByClassFiles(classFiles: List<ClassFile>): Result {
    return classFiles.map { classFile ->
      if (classFile.sourceFile is CompilableSourceFile) {
        Result.withClassFileToPrepare(ClassFilePreparationTask(classFile.file.toNioPath(), classFile.sourceFile))
      }
      else {
        Result.withClassFileToOpen(classFile)
      }
    }.reduce()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun findByPsiElement(psiElement: PsiElement, workingFile: WorkingFile): Result {
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
      }
      else {
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
    val libraryResult: Result = findClassFilesIfOriginalElementIsInLibrary(psiElement, workingFile)
    if (libraryResult.isNotEmpty()) {
      return libraryResult
    }

    // Module descriptor
    val containingModule = psiElement.getParentOfType<PsiJavaModule>(false)
    if (containingModule != null) {
      // A module descriptor does not have nested classes. Therefore, we can
      // directly consume the containing file.
      return workingFile.toResult(Path.of("module-info.class"))
    }

    // Element in a class
    return findClassFilesFromContainingClass(psiElement, workingFile)
  }

  private fun findClassFilesIfOriginalElementIsInLibrary(
    psiElement: PsiElement,
    workingFile: WorkingFile,
    knowingRelativeClassFilePath: Path? = null
  ): Result {
    //    var originalElementContainingFile: PsiFile? = psiElement.originalElement?.containingFile
    //    if (originalElementContainingFile?.virtualFile == null) {
    //      // Opening an element from a library which does not have a source JAR, may
    //      // lead
    //      originalElementContainingFile = psiElement.getContainingClass()?.originalElement?.containingFile
    //    }
    if (projectFileIndex.isInLibrary(workingFile.virtualFile)) {
      // The relative class file path may be a nested class in the `workingFile.virtualFile`
      val relativeClassFilePath = psiElement.findRelativeClassFilePathFromContainingClass() ?: knowingRelativeClassFilePath
      if (relativeClassFilePath != null) {
        // If a file of a library gets opened, the `originalElementContainingFile`
        // will be a source file. One of the elements in that file, probably the
        // first class element, will be linked to the `originalElement` of the
        // class file.
        val classVirtualFile = workingFile.psiFile.children
          .firstNotNullOfOrNull {
            val containingVirtualFile = it.originalElement.containingFile.virtualFile
            if (containingVirtualFile?.isClassFile() == true) containingVirtualFile else null
          }
          ?.parent?.findFile(relativeClassFilePath.fileName.toString())
        if (classVirtualFile != null) {
          val sourceFile = findSource(if (!workingFile.virtualFile.isClassFile()) workingFile.virtualFile else classVirtualFile)
          return findByClassFiles(listOf(ClassFile(file = classVirtualFile, sourceFile = sourceFile)))
        }
      }
    }
    return Result.empty()
  }

  private fun PsiClass.getFqClassName(): String? {
    if (this !is PsiAnonymousClass) {
      return ClassUtil.getJVMClassName(this)
    }

    val containingClass = this.getParentOfType<PsiClass>(false) ?: return null
    return "${containingClass.getFqClassName()}${JavaAnonymousClassesHelper.getName(this)}"
  }

  private fun PsiElement.findRelativeClassFilePathFromContainingClass(): Path? =
    if (containingFile is KtFile) {
      findNonLocalParentKtClassOrObject()?.let { classId ->
        val packagePath = Paths.get("", *classId.packageFqName.pathSegments().map { it.asStringStripSpecialMarkers() }.toTypedArray())
        val fileName = classId.relativeClassName.pathSegments().joinToString(separator = "$", postfix = ".class") { it.asStringStripSpecialMarkers() }
        return packagePath.resolve(fileName)
      }
    }
    else if (containingFile.name.substringBefore(".") == "module-info") {
      Path.of("module-info.class")
    }
    else {
      getParentOfType<PsiClass>(false)?.getFqClassName()?.toRelativeClassFilePath()
    }

  /**
   * If we are in a local class, we can't determine the class file name
   * based on the PSI data model, since it's up to the compiler on how to
   * name the class (e.g., `Foo$method$1`). For this case, we will use
   * the next non-local parent class or object.
   */
  private fun findClassFilesFromContainingClass(psiElement: PsiElement, workingFile: WorkingFile): Result {
    // Kotlin code
    if (workingFile.psiFile is KtFile) {
      val relativeClassFilePath = psiElement.findRelativeClassFilePathFromContainingClass()
      if (relativeClassFilePath != null) {
        return workingFile.toResult(relativeClassFilePath)
      }

      // Fallback to the containing file
      return if (workingFile.isClassFile) {
        // The containing file is a class file and therefore has an unambiguous
        // JVM class.
        val sourceFile = findSource(workingFile.virtualFile)
        Result.withClassFileToOpen(ClassFile(workingFile.virtualFile, sourceFile))
      }
      else {
        // The containing file is a source file. Therefore, we do not have an
        // unambiguous JVM class based on the selected element.
        findClassFilesFromPsiFile(psiElement.containingFile.originalFile, workingFile)
      }
    }

    // Continue with generic PSI module

    // A light method is a synthetic method in an inner enum or record.
    val lightMethod = psiElement.getParentOfType<LightMethod>(false)
    var containingClass = lightMethod?.containingClass

    if (containingClass == null) {
      containingClass = psiElement.getParentOfType<PsiClass>(false)
    }

    while (containingClass is PsiTypeParameter) {
      containingClass = containingClass.getParentOfType<PsiClass>(false)
    }

    if (containingClass == null) {
      return findClassFilesFromPsiFile(workingFile.psiFile, workingFile)
    }

    // Check for local or anonymous class
    if (PsiUtil.isLocalOrAnonymousClass(containingClass)) {
      // We can't get the class file name for a local or anonymous class. Therefore,
      // we have to use the next containing class.
      return if (containingClass.parent != null) {
        findClassFilesFromContainingClass(containingClass.parent, workingFile)
      }
      else {
        Result.withErrorNoContainingFileForElement()
      }
    }

    return findClassFilesFromPsiClass(containingClass, workingFile)
  }

  private fun WorkingFile.toResult(relativeClassFilePath: Path): Result {
    return if (isClassFile) {
      val classVirtualFile = virtualFile.parent.findFileByRelativePath(relativeClassFilePath.fileName.toString())
      if (classVirtualFile != null) {
        Result.withClassFileToOpen(ClassFile(file = classVirtualFile, sourceFile = findSource(virtualFile)))
      }
      else {
        Result.withError("Unable to find class file: $relativeClassFilePath")
      }
    }
    else if (projectFileIndex.isInLibrary(virtualFile)) {
      return findClassFilesIfOriginalElementIsInLibrary(psiFile, this, relativeClassFilePath)
    }
    else {
      findClassFileInCompilerOutputDirOfSourceFile(relativeClassFilePath, virtualFile)
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
      classVirtualFileToUse = virtualFile.parent?.findFile("${virtualFile.name.substringBefore('$')}.class")
        ?: virtualFile
    }

    val psiFile = PsiManager.getInstance(project).findFile(classVirtualFileToUse)
    return if (psiFile is PsiCompiledFile && psiFile is PsiClassOwner) {
      // See `JavaEditorFileSwapper#findSourceFile`
      // Only `ClsClassImpl` have a `sourceMirrorClass` which will lead to the
      // real source file (if it exists). All other `Cls*` instance, for example
      // a module descriptor just have a `mirror` that is not bound to the real
      // source file, only the virtual decompiled PSI file.
      (psiFile as PsiClassOwner).classes
        .takeIf { classes -> classes.isNotEmpty() && classes[0] is ClsClassImpl }
        ?.let { classes -> (classes[0] as ClsClassImpl).sourceMirrorClass?.containingFile?.virtualFile }
        ?.let { SourceFile.NonCompilableSourceFile(it) }
    }
    else if (psiFile is KtDecompiledFile) {
      // See `KotlinEditorFileSwapper#getSourcesLocation`
      psiFile.declarations.firstOrNull()?.let { declaration ->
        serviceOrNull<KotlinDeclarationNavigationPolicy>()?.getNavigationElement(declaration)
          ?.takeIf { it != declaration }
          ?.containingFile
          ?.takeIf { it.isValid }
          ?.virtualFile
          ?.let { SourceFile.NonCompilableSourceFile(it) }
      }
    }
    else {
      null
    }
  }

  private fun findClassFilesFromPsiFile(psiFile: PsiFile, workingFile: WorkingFile): Result {
    // We may reach this point if the selected element is, for example, an
    // input statement. So we are outside a class.
    if (psiFile !is PsiClassOwner) {
      return Result.withErrorNotProcessableFile(psiFile)
    }

    return psiFile.classes
      .map { findClassFilesFromPsiClass(it, workingFile) }
      .reduce()
  }

  private fun findClassFilesFromPsiClass(psiClass: PsiClass, workingFile: WorkingFile): Result {
    val fqClassName = psiClass.getFqClassName() ?: throw IllegalStateException("TODO")

    // If the `PsiClass` is an inner class in a class file, the `containingFile`
    // would return the file of the outermost class file.
    var potentialClassFile: VirtualFile? = psiClass.originalElement?.containingFile?.virtualFile
    val simpleClassName = ClassUtil.extractClassName(fqClassName)
    potentialClassFile = potentialClassFile?.parent?.findFile("$simpleClassName.class")
    if (potentialClassFile != null && potentialClassFile.isClassFile()) {
      val sourceFile = findSource(workingFile.virtualFile)
      return Result.withClassFileToOpen(ClassFile(file = potentialClassFile, sourceFile = sourceFile))
    }

    return workingFile.toResult(fqClassName.toRelativeClassFilePath())
  }

  private fun String.toRelativeClassFilePath(): Path =
    FileSystems.getDefault().getPath("${this.replace('.', '/')}.class")

  private fun findClassFileInCompilerOutputDirOfSourceFile(
    relativeClassFilePath: Path,
    sourceFilePath: VirtualFile
  ): Result = ReadAction.compute<Result, Throwable> {
    assert(relativeClassFilePath.fileName.toString().endsWith(".class"))

    val module = projectFileIndex.getModuleForFile(sourceFilePath)
    if (module != null) {
      val sourceFile = CompilableSourceFile(sourceFilePath, module)
      val expectedClassFilePath: Path? = determineFullClassFilePathInCompilerOutputOfSourceFile(relativeClassFilePath, sourceFile)
      if (expectedClassFilePath != null) {
        return@compute Result.withClassFileToPrepare(ClassFilePreparationTask(expectedClassFilePath, sourceFile))
      }
    }

    return@compute if (DumbService.isDumb(project)) {
      Result.withErrorDumbMode()
    }
    else {
      Result.withError(
        "Couldn't determine the expected class file path for source file '${sourceFilePath.name}'.\n" +
                "Try to recompile the project or, if it's a non-project Java source file, compile the file " +
                "separately and open the resulting class file directly."
      )
    }
  }

  private fun PsiElement.findNonLocalParentKtClassOrObject(): ClassId? {
    var parentKtClassOrObject = getParentOfType<KtClassOrObject>(false)
    // `KtEnumEntry` is a value in an enum and is also a `KtClass` but it can't
    // be reference as a standalone class file. Therefore, we use the actual
    // enum as a parent class. Note the change in the `strict` parameter.
    if (parentKtClassOrObject is KtEnumEntry) {
      parentKtClassOrObject = parentKtClassOrObject.getParentOfType<KtClassOrObject>(true)
    }

    if (parentKtClassOrObject != null && parentKtClassOrObject.classIdIfNonLocal == null) {
      return parentKtClassOrObject.parent.findNonLocalParentKtClassOrObject()
    }

    return parentKtClassOrObject?.classIdIfNonLocal
  }

  private fun determineFullClassFilePathInCompilerOutputOfSourceFile(relativeClassFilePath: Path, sourceFile: CompilableSourceFile): Path? {
    val compiler = CompilerModuleExtension.getInstance(sourceFile.module) ?: return null
    val isTest = projectFileIndex.isInTestSourceContent(sourceFile.file)
    // Don't use 'compilerOutputPath' here because if there is a compiler output
    // path but nothing was compiled yet (and therefore the directory does not
    // exist), it returns null.
    val classRoot = (if (isTest) compiler.compilerOutputForTestsPointer else compiler.compilerOutputPointer) ?: return null
    return Paths.get(classRoot.presentableUrl).resolve(relativeClassFilePath)
  }

  private fun List<Result>.reduce() =
    this.reduceOrNull { a, b -> a.addResult(b) } ?: Result.empty()

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class WorkingFile(val psiFile: PsiFile, val virtualFile: VirtualFile, val isClassFile: Boolean) {

    companion object {

      fun fromPsiElement(psiElement: PsiElement, psiElementOriginPsiFile: PsiFile?): WorkingFile? {
        val psiFile = (psiElement.originalElement.containingFile ?: psiElementOriginPsiFile) ?: return null
        val virtualFile = psiFile.originalFile.virtualFile
        val isClassFile = virtualFile.isClassFile()
        return WorkingFile(psiFile, virtualFile, isClassFile)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class Result(
    val classFilesToOpen: MutableList<ClassFile> = mutableListOf(),
    val classFilesToPrepare: MutableList<ClassFilePreparationTask> = mutableListOf(),
    val errors: MutableList<String> = mutableListOf()
  ) {

    fun addResult(result: Result): Result {
      classFilesToOpen.addAll(result.classFilesToOpen)
      classFilesToPrepare.addAll(result.classFilesToPrepare)
      errors.addAll(result.errors)
      return this
    }

    fun isNotEmpty(): Boolean = classFilesToOpen.isNotEmpty() || classFilesToPrepare.isNotEmpty() || errors.isNotEmpty()

    companion object {

      fun withError(error: String) = Result(errors = mutableListOf(error))

      fun withErrorDumbMode() =
        Result(errors = mutableListOf("Analyse byte code is not available while indices are updating."))

      fun withErrorNoContainingFileForElement() =
        Result(errors = mutableListOf("Unable to determine a source or class file for the selected element."))

      fun withErrorNotProcessableFile(psiFile: PsiFile) =
        Result(errors = mutableListOf("The file '${psiFile.name}' is not a processable source or class file."))

      fun withClassFileToPrepare(classFileToPrepare: ClassFilePreparationTask) =
        Result(classFilesToPrepare = mutableListOf(classFileToPrepare))

      fun withClassFileToOpen(classFile: ClassFile) =
        Result(classFilesToOpen = mutableListOf(classFile))

      fun empty() = Result()
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun fileCanBeAnalysed(file: VirtualFile, project: Project): Boolean {
      if (file.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      if (file.isClassFile()) {
        return true
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(file)
      return psiFile != null && fileCanBeAnalysed(psiFile)
    }

    fun fileCanBeAnalysed(psiFile: PsiFile): Boolean {
      if (psiFile.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      return psiFile is PsiClassOwner
    }

    fun VirtualFile.isClassFile(): Boolean =
      FileTypeRegistry.getInstance().isFileOfType(this, JavaClassFileType.INSTANCE)
  }
}

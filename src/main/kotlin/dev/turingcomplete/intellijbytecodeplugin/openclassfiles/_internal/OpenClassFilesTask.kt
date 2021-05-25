package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.compiler.impl.CompositeScope
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileScope
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import java.nio.file.Path

/**
 * The code of the following methods:
 *
 * * [OpenClassFilesTask#findParentIfLocalOrAnonymousClass]
 * * [OpenClassFilesTask#findExpectedClassFilePath]
 * * [OpenClassFilesTask#findContainingClass]
 *
 * was taken from the class [ByteCodeViewerManager](https://github.com/JetBrains/intellij-community/blob/07c863618cde08055f9662374bbf4c988a322657/plugins/ByteCodeViewer/src/com/intellij/byteCodeViewer/ByteCodeViewerManager.java)
 * in the 'IntelliJ IDEA Community Edition' project, which is licensed under
 * 'Apache License 2.0'.
 */
class OpenClassFilesTask(private val openFile: (VirtualFile) -> Unit, private val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val MESSAGE_DIALOG_TITLE = "Open Class Files"

    fun isOpenableFile(file: VirtualFile, project: Project) : Boolean {
      if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE) && file.isValid) {
        return true
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(file)
      return psiFile is PsiClassOwner
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val errors = mutableListOf<String>()

  private val readyToOpen = mutableListOf<VirtualFile>()
  private val outdatedClassFiles = mutableListOf<ClassFileNeedingPreparation>()
  private val missingClassFiles = mutableListOf<ClassFileNeedingPreparation>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  private val compilerManager by lazy { CompilerManager.getInstance(project) }
  private val projectFileIndex by lazy { ProjectFileIndex.SERVICE.getInstance(project) }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun consumeFiles(files: List<VirtualFile>): OpenClassFilesTask {
    val psiFilesToOpen = mutableListOf<PsiFile>()

    files.forEach { file ->
      if (file.isDirectory) {
        return@forEach
      }

      if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE) && file.isValid) {
        readyToOpen.add(file)
      }
      else {
        val psiFile = PsiManagerEx.getInstance(project).findFile(file)
        if (psiFile is PsiClassOwner) {
          psiFilesToOpen.add(psiFile)
        }
        else {
          errors.add("File '${file.name}' is not a class, a source file (which contains JVM classes) or does not exists.")
        }
      }
    }

    if (psiFilesToOpen.isNotEmpty()) {
      consumePsiFiles(psiFilesToOpen)
    }

    return this
  }

  fun consumePsiFiles(psiFiles: List<PsiFile>): OpenClassFilesTask {
    psiFiles.forEach { psiFile ->
      if (psiFile !is PsiClassOwner || psiFile.classes.isEmpty()) {
        errors.add("Source file '${psiFile.name}' does not contain any JVM classes.")
        return@forEach
      }

      val sourceFile = psiFile.virtualFile
      if (psiFile.virtualFile == null) {
        errors.add("File '${psiFile.name}' is not associated with a physical file.")
        return@forEach
      }

      psiFile.classes.forEach { psiClass -> consumePsiClass(psiClass, sourceFile, psiFile) }
    }

    return this
  }

  fun consumePsiElement(psiElement: PsiElement): OpenClassFilesTask {
    val psiClass = findContainingClass(psiElement)
    if (psiClass == null) {
      errors.add("Couldn't find class for selected element.")
      return this
    }

    val psiFile = psiClass.originalElement.containingFile
    if (psiFile !is PsiJavaFile) {
      errors.add("File '${psiFile.name}' is neither a Java class nor a source file.")
      return this
    }

    val sourceFile = psiFile.virtualFile
    if (sourceFile == null) {
      errors.add("File '${psiFile.name}' is not associated with a physical file.")
      return this
    }

    consumePsiClass(psiClass, sourceFile, psiFile)

    return this
  }

  fun openFiles() {
    if (errors.isNotEmpty()) {
      val message = if (errors.size == 1) errors[0] else errors.joinToString("\n", transform = { "- $it" })
      Messages.showErrorDialog(project, message, MESSAGE_DIALOG_TITLE)
    }

    val compileScope = sequenceOf(prepareForCompilation(outdatedClassFiles,
                                                        "The following source files are not up-to-date: " +
                                                        outdatedClassFiles.joinToString(", ", transform = { it.sourceFile.name }) +
                                                        " should these be compiled?"),
                                  prepareForCompilation(missingClassFiles,
                                                        "No class file could be found for the following source files: " +
                                                        missingClassFiles.joinToString(", ", transform = { it.sourceFile.name }) +
                                                        " should they be compiled?"))
            .filterNotNull()
            .reduceOrNull { first, second -> CompositeScope(first, second) }

    readyToOpen.forEach { openFile(it) }

    if (compileScope != null) {
      compilerManager.compile(compileScope, OpenClassFilesAfterCompilationHandler(outdatedClassFiles.plus(missingClassFiles), openFile))
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun consumePsiClass(psiClass: PsiClass, sourceFile: VirtualFile, psiFile: PsiClassOwner) {
    val jvmClassName = toJvmClassName(psiClass)
    if (jvmClassName == null) {
      errors.add("Couldn't determine the JVM class name of the source file '${psiClass.name}'.")
      return
    }

    // If the source file is from a library, we need the corresponding class file.
    val file = psiClass.originalElement.containingFile?.virtualFile ?: sourceFile

    // 'file' is a class file
    if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE)) {
      val classFileName = "${StringUtil.getShortName(jvmClassName)}.class"
      val classFile = if (projectFileIndex.isInLibraryClasses(file)) {
        file.parent.findChild(classFileName)
      }
      else {
        VirtualFileManager.getInstance().findFileByNioPath(Path.of(file.parent.path, classFileName))
      }

      if (classFile == null || !classFile.isValid) {
        errors.add("Couldn't find class file '${classFileName}' in '${file.parent}'.")
      }
      else {
        readyToOpen.add(classFile)
      }

      return
    }

    // 'file' is a Java source file
    val moduleToExpectedClassFilePath = ReadAction.compute<Pair<Module, Path>?, Throwable> {
      val module = projectFileIndex.getModuleForFile(file)
      val expectedClassFilePath = if (module != null) findExpectedClassFilePath(module, jvmClassName, file) else null
      if (module == null || expectedClassFilePath == null) {
        if (DumbService.isDumb(project)) {
          errors.add("Couldn't determine the expected class file path for source file '${psiFile.name}', because " +
                     "indices are updating.")
        }
        else {
          errors.add("Couldn't determine the expected class file path for source file '${psiFile.name}', because " +
                     "it may not belong to this project.\n" +
                     "Try to compile the file separately and open the resulted class file directly.")
        }
        null
      }
      else {
        Pair(module, expectedClassFilePath)
      }
    } ?: return
    val module = moduleToExpectedClassFilePath.first
    val expectedClassFilePath = moduleToExpectedClassFilePath.second

    val classFile = VirtualFileManager.getInstance().findFileByNioPath(expectedClassFilePath)
    if (classFile?.isValid == false) {
      missingClassFiles.add(ClassFileNeedingPreparation(module, file, expectedClassFilePath))
    }
    else if (!compilerManager.isUpToDate(compilerManager.createFilesCompileScope(arrayOf(file)))) {
      outdatedClassFiles.add(ClassFileNeedingPreparation(module, file, expectedClassFilePath))
    }
    else {
      readyToOpen.add(classFile!!)
    }
  }

  private fun toJvmClassName(psiClass: PsiClass): String? {
    if (psiClass !is PsiAnonymousClass) {
      return ClassUtil.getJVMClassName(psiClass)
    }

    val containingClass = PsiTreeUtil.getParentOfType(psiClass, PsiClass::class.java) ?: return null
    return "${toJvmClassName(containingClass)}${JavaAnonymousClassesHelper.getName(psiClass)}"
  }

  private fun findExpectedClassFilePath(module: Module, jvmClassName: String, sourceFile: VirtualFile): Path? {
    val compiler = CompilerModuleExtension.getInstance(module) ?: return null
    val isTest = projectFileIndex.isInTestSourceContent(sourceFile)
    val classRoot = (if (isTest) compiler.compilerOutputPathForTests else compiler.compilerOutputPath) ?: return null
    val relativePath = "${jvmClassName.replace('.', '/')}.class"
    return Path.of(classRoot.path, relativePath)
  }


  private fun prepareForCompilation(classFilesNeedingPreparation: List<ClassFileNeedingPreparation>, question: String): CompileScope? {
    if (classFilesNeedingPreparation.isEmpty()) {
      return null
    }

    val answer = Messages.showDialog(project, question, MESSAGE_DIALOG_TITLE,
                                     arrayOf("Compile only class(es)",
                                             "Compile whole module(s)",
                                             "Compile whole module(s) and dependent modules",
                                             "Cancel"),
                                     0, null)
    if (answer == -1) {
      return null
    }

    val compilerManager = CompilerManager.getInstance(project)
    return if (answer == 0) {
      compilerManager.createFilesCompileScope(classFilesNeedingPreparation.map { it.sourceFile }.toTypedArray())
    }
    else {
      val includeDependentModules = answer == 2
      compilerManager.createModulesCompileScope(classFilesNeedingPreparation.map { it.module }.toTypedArray(), includeDependentModules)
    }
  }

  private fun findContainingClass(psiElement: PsiElement): PsiClass? {
    var containingClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java, false)

    while (containingClass is PsiTypeParameter) {
      containingClass = PsiTreeUtil.getParentOfType(containingClass, PsiClass::class.java)
    }

    if (containingClass == null) {
      val containingFile = psiElement.containingFile
      if (containingFile !is PsiClassOwner) {
        return null
      }

      val psiClasses = containingFile.classes
      if (psiClasses.size == 1) {
        containingClass = psiClasses[0]
      }
      else {
        val textRange = psiElement.textRange ?: return null
        for (psiClass in psiClasses) {
          val classRange = psiClass.navigationElement.textRange
          if (classRange != null && classRange.contains(textRange)) {
            containingClass = psiClass
          }
        }
      }
    }

    return if (containingClass != null) findParentIfLocalOrAnonymousClass(containingClass) else null
  }

  private fun findParentIfLocalOrAnonymousClass(psiClass: PsiClass): PsiClass {
    var parentClass = psiClass
    while (PsiUtil.isLocalOrAnonymousClass(parentClass)) {
      val nextParentClass = PsiTreeUtil.getParentOfType(parentClass, PsiClass::class.java)
      if (nextParentClass != null) {
        parentClass = nextParentClass
      }
    }
    return parentClass
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ClassFileNeedingPreparation(val module: Module, val sourceFile: VirtualFile, val expectedClassFilePath: Path)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenClassFilesAfterCompilationHandler(private val classFilesNeedingPreparation: List<ClassFileNeedingPreparation>,
                                                      private val openFile: (VirtualFile) -> Unit)
    : CompileStatusNotification {

    override fun finished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
      if (aborted || errors > 0) {
        return
      }

      ApplicationManager.getApplication().invokeLater {
        val virtualFileManager = VirtualFileManager.getInstance()
        classFilesNeedingPreparation.mapNotNull { virtualFileManager.findFileByNioPath(it.expectedClassFilePath) }
                .filter { it.isValid }
                .forEach { openFile(it) }
      }
    }
  }
}
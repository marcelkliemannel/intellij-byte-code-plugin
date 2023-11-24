package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.util.JavaAnonymousClassesHelper
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.ClassFilePreparationTask
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
@Service(Service.Level.PROJECT)
internal class ClassFilesFinderService(private val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val MESSAGE_DIALOG_TITLE = "Open Class Files"

    fun isOpenableFile(file: VirtualFile, project: Project): Boolean {
      if (file.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      if (isReadyToOpen(file)) {
        return true
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(file)
      return psiFile != null && isOpenableFile(psiFile)
    }

    private fun isReadyToOpen(file: VirtualFile): Boolean {
      return FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE) && file.isValid
    }

    fun isOpenableFile(psiFile: PsiFile?): Boolean {
      if (psiFile?.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      return psiFile is PsiClassOwner
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val projectFileIndex by lazy { ProjectFileIndex.getInstance(project) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun findByVirtualFiles(files: List<VirtualFile>, consumeClassFile: (ClassFile) -> Unit) {
    val psiFilesToOpen = mutableListOf<PsiFile>()

    files.forEach { file ->
      if (file.isDirectory) {
        return@forEach
      }

      if (isReadyToOpen(file)) {
        consumeClassFile(ClassFile(file))
        return@forEach
      }

      if (DumbService.isDumb(project)) {
        showError(
          "Couldn't determine if file '${file.name}' is a processable class or source file because indices are updating. " +
                  "Please try again after the indexing finished."
        )
        return@forEach
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(file)
      if (psiFile is PsiClassOwner) {
        psiFilesToOpen.add(psiFile)
      }
      else {
        showError("File '${file.name}' is not a class, a source file (which contains JVM classes) or does not exists.")
      }
    }

    if (psiFilesToOpen.isNotEmpty()) {
      findByPsiFiles(psiFilesToOpen, consumeClassFile, true)
    }
  }

  /**
   * @param doNotProcessAsVirtualFiles true, if the given `psiFiles` should not
   * be processed as [VirtualFile]s by calling [findByVirtualFiles].
   */
  fun findByPsiFiles(psiFiles: List<PsiFile>, consumeClassFile: (ClassFile) -> Unit, doNotProcessAsVirtualFiles: Boolean = false) {
    psiFiles.forEach { psiFile ->
      if (DumbService.isDumb(project)) {
        showError(
          "Couldn't determine if file '${psiFile.name}' is a processable class or source file " +
                  "because indices are updating. Please try again after the indexing finished.",
        )
        return@forEach
      }

      if (psiFile is PsiJavaFile) {
        // package-info.java
        if (psiFile.name == "package-info.java") {
          // Only exists in sources
          return@forEach
        }

        // module-info.java
        val moduleDeclaration = psiFile.moduleDeclaration
        if (moduleDeclaration != null) {
          findByPsiElements(listOf(moduleDeclaration), consumeClassFile)
          return@forEach
        }
      }

      if (psiFile is PsiClassOwner && psiFile.classes.isNotEmpty()) {
        psiFile.classes.forEach { psiClass -> consumePsiClass(psiClass, psiFile, consumeClassFile) }
        return@forEach
      }

      if (!doNotProcessAsVirtualFiles) { // Prevent stack overflow if called from 'consumeFiles()'
        val virtualFileOfPsiFile = psiFile.virtualFile
        if (virtualFileOfPsiFile != null) {
          findByVirtualFiles(listOf(virtualFileOfPsiFile), consumeClassFile)
          return@forEach
        }
      }

      showError(
        "File '${psiFile.name}' is not a processable class or source file. If you tried to open a class " +
                "file from the editor or project view, try to open the compiled '.class' file of the class from " +
                "the compiler output directory directly."
      )
    }
  }

  private fun showError(message: String) {
    Messages.showErrorDialog(project, message, MESSAGE_DIALOG_TITLE)
  }

  fun findByPsiElements(
    psiElements: List<PsiElement>,
    consumeClassFile: (ClassFile) -> Unit,
    originPsiFile: PsiFile? = null,
    originalFile: VirtualFile? = null
  ) {
    if (DumbService.isDumb(project)) {
      if (originalFile != null && isReadyToOpen(originalFile)) {
        // Fallback to the original file
        // Advantage:
        // If the user opens the .class file in the out directory from the
        // project view, IntelliJ would give us a PsiClass element. But if this
        // is happening during indexing, it would not be possible to open the
        // file although the indexing has no effect on the compiled file.
        // Disadvantage:
        // If the user tries to open an inner class of a decompiled .class file
        // during indexing, we would now open the outermost class file.
        consumeClassFile(ClassFile(originalFile))
      }
      else {
        showError("Couldn't process selection because indices are updating. Please try again after the indexing finished.")
      }
      return
    }

    psiElements.forEach { psiElement ->
      if (psiElement is PsiFile) {
        findByPsiFiles(listOf(psiElement), consumeClassFile)
        return@forEach
      }

      // - Try to consume as element in module descriptor

      val containingModule = PsiTreeUtil.getParentOfType(psiElement, PsiJavaModule::class.java, false)
      if (containingModule != null) {
        consumeJavaVirtualFile(psiElement, psiElement.containingFile, "module-info", consumeClassFile, originPsiFile)
        return@forEach
      }

      // - Try to consume as element in class

      // If the element is in a synthetic method in an inner enum or record,
      // `findNextPsiClass` would find the outer class as the parent PSI class.
      val lightMethod = PsiTreeUtil.getParentOfType(psiElement, LightMethod::class.java, false)
      val psiClass = lightMethod?.containingClass ?: findNextPsiClass(psiElement)
      if (psiClass == null) {
        // If the element is outside a class (e.g. the white space after the
        // last closing bracket) we use the containing file as a fallback.
        val fallbackClassFile = psiElement.containingFile ?: originPsiFile
        if (fallbackClassFile != null) {
          findByPsiFiles(listOf(fallbackClassFile), consumeClassFile)
        }
        else {
          showError("Couldn't find a class for the selected element.")
        }
        return@forEach
      }

      val psiFile = psiClass.originalElement.containingFile
      if (psiFile !is PsiJavaFile) {
        showError("File '${psiFile.name}' is neither a Java class nor a source file.")
        return@forEach
      }

      consumePsiClass(psiClass, psiFile, consumeClassFile)
    }
  }

  fun findByClassFiles(classFiles: List<ClassFile>, consumeClassFile: (ClassFile) -> Unit) {
    // Consume class files without a source file directly
    classFiles.filter { it.sourceFile == null }.forEach {
      consumeClassFile(it)
    }

    // Prepare class files with a source file
    val classFilePreparationTasks = classFiles.filter { it.sourceFile != null }.map {
      ClassFilePreparationTask(it.file.toNioPath(), it.sourceFile!!)
    }
    project.getService(ClassFilesPreparatorService::class.java)
      .prepareClassFiles(classFilePreparationTasks, null, consumeClassFile)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun consumePsiClass(psiClass: PsiClass, sourcePsiFile: PsiFile, consumeClassFile: (ClassFile) -> Unit) {
    val jvmClassName = toJvmClassName(psiClass)
    if (jvmClassName == null) {
      Messages.showErrorDialog(project, "Couldn't determine the JVM class name of the source file '${psiClass.name}'.", MESSAGE_DIALOG_TITLE)
      return
    }

    consumeJavaVirtualFile(psiClass, sourcePsiFile, jvmClassName, consumeClassFile)
  }

  private fun consumeJavaVirtualFile(psiElement: PsiElement?,
                                     sourcePsiFile: PsiFile,
                                     jvmFileName: String,
                                     consumeClassFile: (ClassFile) -> Unit,
                                     editorPsiFile: PsiFile? = null) {

    val sourceFile = sourcePsiFile.virtualFile
    if (sourceFile == null) {
      showError("Virtual file '${sourcePsiFile.name}' is not associated with a physical file.")
      return
    }

    val file = psiElement?.originalElement?.containingFile?.virtualFile ?: sourceFile

    // -- Check if 'file' is a class file --

    if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE)) {
      // This code is reached if a compiled class file is opened in the editor,
      // and the action is executed outgoing from a PSI element.

      // The 'file' at this moment is the class file of the outermost class,
      // but if the 'psiElement' is in or is an inner class, we need the
      // class file of that inner class.
      val classFileName = "${StringUtil.getShortName(jvmFileName)}.class"
      val classFile = file.parent.findChild(classFileName)
      if (classFile == null || !classFile.isValid) {
        Messages.showErrorDialog(project, "Couldn't find class file: $file", MESSAGE_DIALOG_TITLE)
      }
      else {
        consumeClassFile(ClassFile(classFile))
      }
    }

    // -- Check if 'file' is a Java source file --

    // There is an edge case there the 'psiElement' is from a library class file,
    // but the 'psiElement?.originalElement?.containingFile', which was used to
    // determine the 'file' instance, returns the path to the Java source file
    // and not the original class file. This case occurs at least for the
    // 'module-info.class' of JDK modules in IntelliJ 2021.1. Since it's obviously
    // not possible to compile such files (which would be tried in the next block),
    // we use as a fall back the current opened file in the editor, which was
    // used to find the 'psiElement' in the first place.
    if (projectFileIndex.isInLibrary(file) && editorPsiFile != null) {
      findByPsiFiles(listOf(editorPsiFile), consumeClassFile)
      return
    }

    val moduleToExpectedClassFilePath = ReadAction.compute<Pair<Module, Path>?, Throwable> {
      val module = projectFileIndex.getModuleForFile(file)
      val expectedClassFilePath = if (module != null) findExpectedClassFilePath(module, jvmFileName, file) else null
      if (module == null || expectedClassFilePath == null) {
        if (DumbService.isDumb(project)) {
          showError(
            "Couldn't determine the expected class file path for source file '${sourceFile.name}' because " +
                    "indices are updating. Please try again after the indexing finished."
          )
        }
        else {
          showError(
            "Couldn't determine the expected class file path for source file '${sourceFile.name}'.\n" +
                    "Try to recompile the project or, if its a non-project Java source file, compile the file " +
                    "separately and open the resulted class file directly."
          )
        }
        null
      }
      else {
        Pair(module, expectedClassFilePath)
      }
    } ?: return
    val expectedClassFilePath = moduleToExpectedClassFilePath.second
    val module = moduleToExpectedClassFilePath.first

    project.getService(ClassFilesPreparatorService::class.java)
      .prepareClassFiles(listOf(ClassFilePreparationTask(expectedClassFilePath, SourceFile(sourceFile, module))), null, consumeClassFile)
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
    // Don't use 'compilerOutputPath' here because if there is a compiler output
    // path but nothing was compiled yet (and therefore the directory does not
    // exist), it returns null.
    val classRoot = (if (isTest) compiler.compilerOutputForTestsPointer else compiler.compilerOutputPointer) ?: return null
    val relativePath = "${jvmClassName.replace('.', '/')}.class"
    return Path.of(classRoot.presentableUrl, relativePath)
  }

  private fun findNextPsiClass(psiElement: PsiElement): PsiClass? {
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
}
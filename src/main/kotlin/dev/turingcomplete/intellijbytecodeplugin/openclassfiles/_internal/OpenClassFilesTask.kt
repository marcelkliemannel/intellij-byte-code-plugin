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
import com.intellij.openapi.diagnostic.Logger
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
import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.util.ClassUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindow.Companion.PLUGIN_NAME
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
    private val LOGGER = Logger.getInstance(OpenClassFilesTask::class.java)

    private const val MESSAGE_DIALOG_TITLE = "Open Class Files"

    fun isOpenableFile(file: VirtualFile, project: Project): Boolean {
      if (file.name == "package-info.java") {
        // Only exists in sources
        return false
      }

      if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE) && file.isValid) {
        return true
      }

      val psiFile = PsiManagerEx.getInstance(project).findFile(file)
      return psiFile != null && isOpenableFile(psiFile)
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
      if (psiFile is PsiJavaFile) {
        // package-info.java
        if (psiFile.name == "package-info.java") {
          // Only exists in sources
          return@forEach
        }

        // module-info.java
        val moduleDeclaration = psiFile.moduleDeclaration
        if (moduleDeclaration != null) {
          consumePsiElements(listOf(moduleDeclaration))
          return@forEach
        }
      }

      if (psiFile is PsiClassOwner) {
        psiFile.classes.forEach { psiClass ->
          consumePsiClass(psiClass, psiFile)
        }
        return@forEach
      }

      errors.add("File '${psiFile.name}' is not a processable source file.")
    }

    return this
  }

  fun consumePsiElements(psiElements: List<PsiElement>): OpenClassFilesTask {
    psiElements.forEach { psiElement ->
      // Consume as element in module descriptor
      val containingModule = PsiTreeUtil.getParentOfType(psiElement, PsiJavaModule::class.java, false)
      if (containingModule != null) {
        consumeJavaVirtualFile(psiElement, psiElement.containingFile, "module-info")
        return@forEach
      }

      // Consume as element in class
      var psiClass: PsiClass?

      // If the element is in a synthetic method in an inner enum or record,
      // `findNextPsiClass` would find the outer class as the parent PSI class.
      val lightMethod = PsiTreeUtil.getParentOfType(psiElement, LightMethod::class.java, false)
      if (lightMethod != null) {
        psiClass = lightMethod.containingClass
      }
      else {
        psiClass = findNextPsiClass(psiElement)
      }

      if (psiClass == null) {
        errors.add("Couldn't find class for selected element.")
        return@forEach
      }

      val psiFile = psiClass.originalElement.containingFile
      if (psiFile !is PsiJavaFile) {
        errors.add("File '${psiFile.name}' is neither a Java class nor a source file.")
        return this
      }

      consumePsiClass(psiClass, psiFile)
    }

    return this
  }

  fun openFiles() {
    if (errors.isNotEmpty()) {
      val message = if (errors.size == 1) {
        "${errors[0]}\n\nPlease create a bug for the $PLUGIN_NAME plugin if this error should not occur."
      }
      else {
        errors.joinToString("\n", transform = { "- $it" }) +
        "\n\nPlease create a bug for the $PLUGIN_NAME plugin if one of these errors should not occur."
      }
      Messages.showErrorDialog(project, message, MESSAGE_DIALOG_TITLE)
      return
    }

    val compileScope = sequenceOf(prepareForCompilation(outdatedClassFiles,
                                                        "The following source file(s) are not up-to-date: " +
                                                        outdatedClassFiles.joinToString(", ", transform = { it.sourceFile.name }) +
                                                        " should it/these be compiled?",
                                                        true),
                                  prepareForCompilation(missingClassFiles,
                                                        "No class file could be found for the following source file(s): " +
                                                        missingClassFiles.joinToString(", ", transform = { it.sourceFile.name }) +
                                                        " should it/they be compiled?",
                                                        false))
            .filterNotNull()
            .reduceOrNull { first, second -> CompositeScope(first, second) }

    readyToOpen.forEach { openFile(it) }

    if (compileScope != null) {
      compilerManager.compile(compileScope, OpenClassFilesAfterCompilationHandler(outdatedClassFiles.plus(missingClassFiles), openFile))
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun consumePsiClass(psiClass: PsiClass, sourcePsiFile: PsiFile) {
    val jvmClassName = toJvmClassName(psiClass)
    if (jvmClassName == null) {
      errors.add("Couldn't determine the JVM class name of the source file '${psiClass.name}'.")
      return
    }

    consumeJavaVirtualFile(psiClass, sourcePsiFile, jvmClassName)
  }

  private fun consumeJavaVirtualFile(psiElement: PsiElement?, sourcePsiFile: PsiFile, jvmFileName: String) {
    val sourceFile = sourcePsiFile.virtualFile
    if (sourceFile == null) {
      errors.add("Virtual file '${sourcePsiFile.name}' is not associated with a physical file.")
      return
    }

    // If the source file is from a library, we need the corresponding class file.
    val file = psiElement?.originalElement?.containingFile?.virtualFile ?: sourceFile

    // 'file' is a class file
    if (FileTypeRegistry.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE)) {
      // This code is reached if a compiled class file is opened in the editor,
      // and the action is executed outgoing from a PSI element.

      // The 'file' at this moment is the class file of the outermost class,
      // but if the 'psiElement' is in or is an inner class, we need the
      // class file of that inner class.
      val classFileName = "${StringUtil.getShortName(jvmFileName)}.class"
      val classFile = file.parent.findChild(classFileName)
      if (classFile == null || !classFile.isValid) {
        errors.add("Couldn't find class file: $file")
      }
      else {
        readyToOpen.add(classFile)
      }

      return
    }

    // 'file' is a Java source file
    val moduleToExpectedClassFilePath = ReadAction.compute<Pair<Module, Path>?, Throwable> {
      val module = projectFileIndex.getModuleForFile(file)
      val expectedClassFilePath = if (module != null) findExpectedClassFilePath(module, jvmFileName, file) else null
      if (module == null || expectedClassFilePath == null) {
        if (DumbService.isDumb(project)) {
          errors.add("Couldn't determine the expected class file path for source file '${sourceFile.name}', because " +
                     "indices are updating.")
        }
        else {
          errors.add("Couldn't determine the expected class file path for source file '${sourceFile.name}', because " +
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
    else if (classFile != null) {
      readyToOpen.add(classFile)
    }
    else {
      LOGGER.warn("Couldn't find virtual file for expected class file: $expectedClassFilePath")
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


  private fun prepareForCompilation(classFilesNeedingPreparation: List<ClassFileNeedingPreparation>,
                                    question: String,
                                    tryToUseClassesAsTheyAre: Boolean): CompileScope? {

    if (classFilesNeedingPreparation.isEmpty()) {
      return null
    }

    val options = mutableListOf<String>()
    if (tryToUseClassesAsTheyAre) {
      options.add("Try to use class(es) as they are")
    }
    options.add("Compile only class(es)")
    val compileOnlyClassesAnswerIndex = options.size - 1

    options.add("Compile whole module(s)")
    val compileWholeModulesAnswerIndex = options.size - 1

    options.add("Compile whole module(s) and dependent modules")
    val compileWholeModulesAndDependentModulesAnswerIndex = options.size - 1

    options.add("Cancel")
    val cancelAnswerIndex = options.size - 1

    val answer = Messages.showDialog(project, question, MESSAGE_DIALOG_TITLE, options.toTypedArray(), 0, null)
    return if (answer == -1 || answer == cancelAnswerIndex) {
      null
    }
    else if (answer == compileOnlyClassesAnswerIndex) {
      CompilerManager.getInstance(project).createFilesCompileScope(classFilesNeedingPreparation.map { it.sourceFile }.toTypedArray())
    }
    else if (answer == compileWholeModulesAnswerIndex || answer == compileWholeModulesAndDependentModulesAnswerIndex) {
      val includeDependentModules = answer == compileWholeModulesAndDependentModulesAnswerIndex
      CompilerManager.getInstance(project).createModulesCompileScope(classFilesNeedingPreparation.map { it.module }.toTypedArray(), includeDependentModules)
    }
    else {
      if (tryToUseClassesAsTheyAre) {
        classFilesNeedingPreparation
                .mapNotNull { VirtualFileManager.getInstance().findFileByNioPath(it.expectedClassFilePath) }
                .filter { it.isValid }
                .forEach { readyToOpen.add(it) }
      }

      null
    }
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
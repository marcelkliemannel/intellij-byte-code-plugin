package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesFinderService
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesFinderService.Result
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService

@Service(Service.Level.PROJECT)
class ByteCodeAnalyserOpenClassFileService(val project: Project) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>) {
    run { service -> service.findByPsiFiles(psiFiles) }
  }

  fun openPsiElements(psiElementToPsiElementOriginPsiFile: Map<PsiElement, PsiFile?>) {
    run { service -> service.findByPsiElements(psiElementToPsiElementOriginPsiFile) }
  }

  fun openVirtualFiles(files: List<VirtualFile>) {
    run { service -> service.findByVirtualFiles(files) }
  }

  internal fun openClassFiles(classFiles: List<ClassFile>) {
    run { service -> service.findByClassFiles(classFiles) }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun run(findBy: (ClassFilesFinderService) -> Result) {
    val result = findBy(project.getService(ClassFilesFinderService::class.java))
    handleResult(result)
  }

  private fun handleResult(result: Result) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ByteCodeToolWindowFactory.TOOL_WINDOW_ID)
      ?: throw IllegalStateException("Could not find tool window '${ByteCodeToolWindowFactory.TOOL_WINDOW_ID}'")

    result.classFilesToOpen.forEach {
      ByteCodeToolWindowFactory.openClassFile(it, toolWindow, project)
    }

    if (result.classFilesToPrepare.isNotEmpty()) {
      val classFilesPreparatorService = project.getService(ClassFilesPreparatorService::class.java)
      classFilesPreparatorService.prepareClassFiles(result.classFilesToPrepare, toolWindow.component) {
        ByteCodeToolWindowFactory.openClassFile(it, toolWindow, project)
      }
    }

    if (result.errors.isNotEmpty()) {
      val errorMessage = if (result.errors.size == 1) {
        result.errors[0]
      }
      else {
        "The following errors occurred: ${result.errors.joinToString(prefix = "<ul>", postfix = "</ul>") { "<li>$it</li>" }}"
      }
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(project, errorMessage, "Analyse Class Files")
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

}

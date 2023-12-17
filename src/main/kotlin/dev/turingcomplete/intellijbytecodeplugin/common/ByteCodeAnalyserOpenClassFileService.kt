package dev.turingcomplete.intellijbytecodeplugin.common

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
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>) {
    project.getService(ClassFilesFinderService::class.java)
      .findByPsiFiles(psiFiles)
  }

  fun openPsiElements(psiElements: Map<PsiElement, PsiFile?>) {
    psiElements.map { (psiElement, psiElementOriginPsiFile) ->
      project.getService(ClassFilesFinderService::class.java)
        .findByPsiElement(psiElement, psiElementOriginPsiFile)
        .let { handleResult(it) }
    }
  }

  fun openVirtualFiles(files: List<VirtualFile>) {
    project.getService(ClassFilesFinderService::class.java)
      .findByVirtualFiles(files)
      .let { handleResult(it) }
  }

  internal fun openClassFiles(classFiles: List<ClassFile>) {
    project.getService(ClassFilesFinderService::class.java)
      .findByClassFiles(classFiles)
      .let { handleResult(it) }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

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
      Messages.showErrorDialog(project, errorMessage, "Analyse Class Files")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

}
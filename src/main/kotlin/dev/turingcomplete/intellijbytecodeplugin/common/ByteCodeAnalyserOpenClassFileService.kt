package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.OpenClassFilesTask

@Service(Service.Level.PROJECT)
class ByteCodeAnalyserOpenClassFileService(val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val openClassFile: (VirtualFile) -> Unit = {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ByteCodeToolWindowFactory.TOOL_WINDOW_ID)
                     ?: throw IllegalStateException("Could not find tool window '${ByteCodeToolWindowFactory.TOOL_WINDOW_ID}'")
    ByteCodeToolWindowFactory.openClassFile(it, toolWindow, project)
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>) {
    OpenClassFilesTask(openClassFile, project).consumePsiFiles(psiFiles).openFiles()
  }

  fun openPsiElements(psiElements: List<PsiElement>, originPsiFile: PsiFile? = null, originalFile: VirtualFile? = null) {
    OpenClassFilesTask(openClassFile, project).consumePsiElements(psiElements, originPsiFile, originalFile).openFiles()
  }

  fun openFiles(files: List<VirtualFile>) {
    OpenClassFilesTask(openClassFile, project).consumeFiles(files).openFiles()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
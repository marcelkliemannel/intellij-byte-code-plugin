package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesFinderService

@Service(Service.Level.PROJECT)
class ByteCodeAnalyserOpenClassFileService(val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val openClassFile: (ClassFile) -> Unit = {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ByteCodeToolWindowFactory.TOOL_WINDOW_ID)
                     ?: throw IllegalStateException("Could not find tool window '${ByteCodeToolWindowFactory.TOOL_WINDOW_ID}'")
    ByteCodeToolWindowFactory.openClassFile(it, toolWindow, project)
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>) {
    project.getService(ClassFilesFinderService::class.java).findByPsiFiles(psiFiles, openClassFile)
  }

  fun openPsiElements(psiElements: List<PsiElement>, originPsiFile: PsiFile? = null, originalFile: VirtualFile? = null) {
    project.getService(ClassFilesFinderService::class.java).findByPsiElements(psiElements, openClassFile, originPsiFile, originalFile)
  }

  fun openFiles(files: List<VirtualFile>) {
    project.getService(ClassFilesFinderService::class.java).findByVirtualFiles(files, openClassFile)
  }

  internal fun openClassFiles(classFiles: List<ClassFile>) {
    project.getService(ClassFilesFinderService::class.java).findByClassFiles(classFiles, openClassFile)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

}
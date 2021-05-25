package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtilBase
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesListener


class AnalyzeBytecodeAction : DumbAwareAction("Analyze Byte Code") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = CommonDataKeys.PROJECT.getData(e.dataContext)?.let { project ->
      val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
      if (files?.any { OpenClassFilesTask.isOpenableFile(it, project) } == true) {
        true
      }
      else {
        findPsiElement(project, e.dataContext)?.let { psiElement ->
          psiElement.containingFile is PsiClassOwner
        }
      }
    } ?: false
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: return

    val files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)
    if (files != null) {
      project.messageBus.syncPublisher(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC).openFiles(files.toList())
      return
    }

    val psiElement = findPsiElement(project, e.dataContext) ?: return
    project.messageBus.syncPublisher(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC).openPsiElement(psiElement)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun findPsiElement(project: Project, dataContext: DataContext): PsiElement? {
    val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return dataContext.getData(CommonDataKeys.PSI_ELEMENT)

    val editorPsiFile = PsiUtilBase.getPsiFileInEditor(editor, project)
    return findPsiElementInInjectedEditor(editor, editorPsiFile, project) ?: editorPsiFile?.findElementAt(editor.caretModel.offset)
  }

  private fun findPsiElementInInjectedEditor(editor: Editor, editorPsiFile: PsiFile?, project: Project): PsiElement? {
    if (editorPsiFile == null || editor is EditorWindow) {
      return null
    }

    val offset = editor.caretModel.offset
    return InjectedLanguageManager.getInstance(project).findInjectedElementAt(editorPsiFile, offset)
            ?.containingFile
            ?.findElementAt(editor.caretModel.offset)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
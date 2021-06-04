package dev.turingcomplete.intellijbytecodeplugin.view.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

class OpenInEditorAction(@Nullable @NlsActions.ActionText text: String = "Open in Editor",
                         icon: Icon = AllIcons.Actions.MoveTo2) : DumbAwareAction(text, null, icon) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: throw IllegalStateException("snh: Missing data")

    val openInEditorFile = dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.getData(e.dataContext)
                           ?: throw IllegalStateException("snh: Missing data")

    FileEditorManager.getInstance(project).openEditor(OpenFileDescriptor(project, openInEditorFile), true)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
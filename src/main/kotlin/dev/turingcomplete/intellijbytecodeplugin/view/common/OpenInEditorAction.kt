package dev.turingcomplete.intellijbytecodeplugin.view.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.DataProviderUtils
import org.jetbrains.annotations.Nullable
import javax.swing.Icon

class OpenInEditorAction(@Nullable @NlsActions.ActionText text: String = "Open in Editor",
                         icon: Icon = AllIcons.Actions.MoveTo2) : DumbAwareAction(text, null, icon) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.getData(e.dataContext) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val openInEditorFile = CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.getData(e.dataContext) ?: return
    val project = DataProviderUtils.getData(com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT, e.dataContext)
    FileEditorManager.getInstance(project).openEditor(OpenFileDescriptor(project, openInEditorFile), true)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
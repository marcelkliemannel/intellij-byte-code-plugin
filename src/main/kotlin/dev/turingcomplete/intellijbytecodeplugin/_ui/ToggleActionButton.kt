package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.DumbAwareActionButton
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.EmptyIcon

internal class ToggleActionButton(
  title: String,
  private val setValue: () -> Unit,
  private val isSelected: () -> Boolean
) : DumbAwareActionButton(title) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    setValue()
  }

  override fun updateButton(e: AnActionEvent) {
    e.presentation.icon = if (isSelected()) {
      PlatformIcons.CHECK_ICON
    }
    else {
      EmptyIcon.create(PlatformIcons.CHECK_ICON)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
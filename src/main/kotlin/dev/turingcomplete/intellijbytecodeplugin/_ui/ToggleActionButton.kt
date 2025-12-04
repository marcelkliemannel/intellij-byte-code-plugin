package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.EmptyIcon

internal class ToggleActionButton(
  title: String,
  private val setValue: () -> Unit,
  private val isSelected: () -> Boolean,
) : DumbAwareAction(title) {

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.icon = if (isSelected()) PlatformIcons.CHECK_ICON else UNCHECKED_ICON
  }

  override fun actionPerformed(e: AnActionEvent) {
    setValue()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val UNCHECKED_ICON = EmptyIcon.create(PlatformIcons.CHECK_ICON)
  }
}

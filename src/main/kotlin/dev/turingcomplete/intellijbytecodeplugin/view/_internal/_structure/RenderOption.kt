package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.DumbAwareActionButton
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.EmptyIcon

class RenderOption(renderModeName: String,
                   private val setValue: () -> Unit,
                   private val isSelected: () -> Boolean) : DumbAwareActionButton(renderModeName) {

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

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.DataProviderUtils

class ViewValueAction : DumbAwareAction("View Value") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = CommonDataKeys.VALUE.getData(e.dataContext) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val value = DataProviderUtils.getData(CommonDataKeys.VALUE, e.dataContext)
    UiUtils.PopUp.showTextAreaPopup(value, e.dataContext)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
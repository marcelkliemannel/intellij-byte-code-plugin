package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.PlatformIcons
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.DataProviderUtils
import java.awt.datatransfer.StringSelection

class CopyValueAction : DumbAwareAction("Copy Value", null, PlatformIcons.COPY_ICON) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = CommonDataKeys.VALUE.getData(e.dataContext) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val value = DataProviderUtils.getData(CommonDataKeys.VALUE, e.dataContext)
    CopyPasteManager.getInstance().setContents(StringSelection(value))
  }

  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
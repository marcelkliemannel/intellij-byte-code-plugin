package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.DataProviderUtils
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction

@Suppress("ComponentNotRegistered")
internal class ReParseByteCodeAction :
  ByteCodeAction("Re-Parse Class File", null, AllIcons.Actions.Refresh) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    val classFileTab =
      DataProviderUtils.getData(CommonDataKeys.CLASS_FILE_TAB_DATA_KEY, e.dataContext)
    classFileTab.reParseClassNodeContext()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

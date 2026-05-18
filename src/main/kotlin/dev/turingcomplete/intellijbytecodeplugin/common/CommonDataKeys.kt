package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DataSnapshot
import com.intellij.openapi.actionSystem.UiDataRule
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin._ui.ClassFileTab

/**
 * If the tool window is open but the focus is outside it (e.g. in the editor) and an [AnAction]
 * inside the tool window is executed, the [DataProvider] inside the tool window is not called
 * because the DataProvider is searched starting from the focused component. Therefore, these
 * [DataKey]s are also registered globally as [UiDataRule]s.
 */
object CommonDataKeys {
  // -- Properties ---------------------------------------------------------- //

  /** The data key is also provided by [ByteCodeToolWindowDataRule] in the `plugin.xml`. */
  val CLASS_FILE_CONTEXT_DATA_KEY =
    DataKey.create<ClassFileContext>("dev.turingcomplete.intellijbytecodeplugin.classFileContext")

  /** The data key is also provided by [ByteCodeToolWindowDataRule] in the `plugin.xml`. */
  internal val CLASS_FILE_TAB_DATA_KEY =
    DataKey.create<ClassFileTab>("dev.turingcomplete.intellijbytecodeplugin.classFileTab")

  /** The data key is also provided by [ByteCodeToolWindowDataRule] in the `plugin.xml`. */
  val ON_ERROR_DATA_KEY =
    DataKey.create<(String, Throwable) -> Unit>("dev.turingcomplete.intellijbytecodeplugin.onError")

  /** The data key is also provided by [ByteCodeToolWindowDataRule] in the `plugin.xml`. */
  val OPEN_IN_EDITOR_DATA_KEY =
    DataKey.create<VirtualFile>("dev.turingcomplete.intellijbytecodeplugin.openInEditor")

  val VALUE = DataKey.create<String>("dev.turingcomplete.intellijbytecodeplugin.value")

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class ByteCodeToolWindowDataRule : UiDataRule {

    override fun uiDataSnapshot(sink: DataSink, snapshot: DataSnapshot) {
      sink.lazyValue(CLASS_FILE_CONTEXT_DATA_KEY) { dataProvider ->
        ByteCodeToolWindowFactory.getData(dataProvider, CLASS_FILE_CONTEXT_DATA_KEY)
      }
      sink.lazyValue(CLASS_FILE_TAB_DATA_KEY) { dataProvider ->
        ByteCodeToolWindowFactory.getData(dataProvider, CLASS_FILE_TAB_DATA_KEY)
      }
      sink.lazyValue(ON_ERROR_DATA_KEY) { dataProvider ->
        ByteCodeToolWindowFactory.getData(dataProvider, ON_ERROR_DATA_KEY)
      }
      sink.lazyValue(OPEN_IN_EDITOR_DATA_KEY) { dataProvider ->
        ByteCodeToolWindowFactory.getData(dataProvider, OPEN_IN_EDITOR_DATA_KEY)
      }
    }
  }
}

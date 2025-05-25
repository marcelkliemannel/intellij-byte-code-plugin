package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.ide.impl.dataRules.GetDataRule
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin._ui.ClassFileTab

/**
 * If the tool window is open but the focus is outside it (e.g. in the editor)
 * and an [AnAction] inside the tool window is executed, the [DataProvider] inside
 * the tool window is not called because the DataProvider is searched starting
 * from the focused component. Therefore, the two [DataKey]s are also registered
 * globally as [GetDataRule]s.
 */
object CommonDataKeys {
  // -- Properties ---------------------------------------------------------- //

  /**
   * The data key is also hard coded for the [ClassFileContextDataRule] in the `plugin.xml`.
   */
  val CLASS_FILE_CONTEXT_DATA_KEY = DataKey.create<ClassFileContext>("dev.turingcomplete.intellijbytecodeplugin.classFileContext")

  /**
   * The data key is also hard coded for the [ClassFileTabDataRule] in the `plugin.xml`.
   */
  internal val CLASS_FILE_TAB_DATA_KEY = DataKey.create<ClassFileTab>("dev.turingcomplete.intellijbytecodeplugin.classFileTab")

  /**
   * The data key is also hard coded for the [OnErrorDataRule] in the `plugin.xml`.
   */
  val ON_ERROR_DATA_KEY = DataKey.create<(String, Throwable) -> Unit>("dev.turingcomplete.intellijbytecodeplugin.onError")

  /**
   * The data key is also hard coded for the [OpenInEditorDataRule] in the `plugin.xml`.
   */
  val OPEN_IN_EDITOR_DATA_KEY = DataKey.create<VirtualFile>("dev.turingcomplete.intellijbytecodeplugin.openInEditor")

  val VALUE = DataKey.create<String>("dev.turingcomplete.intellijbytecodeplugin.value")

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class ClassFileContextDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return CLASS_FILE_CONTEXT_DATA_KEY.getData(dataProvider)
             ?: ByteCodeToolWindowFactory.getData(dataProvider, CLASS_FILE_CONTEXT_DATA_KEY)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class ClassFileTabDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return CLASS_FILE_TAB_DATA_KEY.getData(dataProvider)
             ?: ByteCodeToolWindowFactory.getData(dataProvider, CLASS_FILE_TAB_DATA_KEY)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class OnErrorDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return ON_ERROR_DATA_KEY.getData(dataProvider)
        ?: ByteCodeToolWindowFactory.getData(dataProvider, ON_ERROR_DATA_KEY)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class OpenInEditorDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return OPEN_IN_EDITOR_DATA_KEY.getData(dataProvider)
        ?: ByteCodeToolWindowFactory.getData(dataProvider, OPEN_IN_EDITOR_DATA_KEY)
    }
  }
}

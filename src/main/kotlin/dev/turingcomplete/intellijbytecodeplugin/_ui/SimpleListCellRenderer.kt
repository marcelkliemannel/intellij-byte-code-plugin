package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class SimpleListCellRenderer(private val textTransformer: (Any?) -> String): DefaultListCellRenderer() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focused: Boolean): Component {
    super.getListCellRendererComponent(list, value, index, selected, false)

    text = textTransformer(value)

    border = JBUI.Borders.empty(0, 5, 0, 10)
    if (!selected) {
      background = UIUtil.getLabelBackground()
    }

    return this
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}

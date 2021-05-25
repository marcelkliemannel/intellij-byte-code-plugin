package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.AddEditDeleteListPanel
import com.intellij.ui.components.JBTextField
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

abstract class EditorField<T>(val preText: String?, private val setValue: (T) -> Unit) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createFieldComponent(): JComponent

  protected abstract fun value(): T

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Text(preText: String, private val getValue: () -> String?, setValue: (String) -> Unit)
    : EditorField<String>(preText, setValue) {

    private val textField by lazy { JBTextField(getValue()) }

    override fun createFieldComponent() = textField

    override fun value(): String = textField.text
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Number(preText: String, private val getValue: () -> Int, setValue: (Int) -> Unit)
    : EditorField<Int>(preText, setValue) {

    private val numberField by lazy { UiUtils.Field.createNumberField(getValue()) }

    override fun createFieldComponent() = numberField

    override fun value(): Int = numberField.text.toInt()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Selection<E>(preText: String,
                  private val values: Array<E>,
                  private val getValue: () -> E?,
                  setValue: (E) -> Unit) : EditorField<E>(preText, setValue) {

    private val comboBox by lazy { ComboBox<E>(values) }

    override fun createFieldComponent(): JComponent {
      getValue()?.let { comboBox.selectedItem = it }
      return comboBox
    }

    override fun value(): E = values[comboBox.selectedIndex]
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ItemsList<E>(private val title: String,
                     private val getValue: () -> List<E>,
                     setValue: (List<E>) -> Unit,
                     private val toDisplayValue: (E) -> String = { it.toString() }) : EditorField<List<E>>(null, setValue) {

    private val list by lazy { ItemsListPanel(title, getValue, toDisplayValue) }

    override fun createFieldComponent(): JComponent = list

    override fun value(): List<E> = list.listItems.map { it as E }.toList()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ItemsListPanel<E>(title: String?, getValue: () -> List<E>, private val transformText: (E) -> String)
    : AddEditDeleteListPanel<E>(title, getValue()) {

    init {
      minimumSize = Dimension(minimumSize.width, 80)
    }

    override fun findItemToAdd(): E? {
      TODO("Not yet implemented")
    }

    override fun editSelectedItem(item: E?): E? {
      TODO("Not yet implemented")
    }

    override fun getListCellRenderer(): ListCellRenderer<*> {
      return object: DefaultListCellRenderer() {
        override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
          super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
          text = transformText(value as E)
          return this
        }
      }
    }
  }
}
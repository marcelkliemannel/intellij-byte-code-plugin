package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.util.castSafelyTo
import javax.swing.DefaultComboBoxModel

internal class OpcodeComboBox(opcodes: Array<Pair<Int, String>>, initialSelectedOpcode: Int? = null): ComboBox<Pair<Int, String>>(opcodes) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    setRenderer(SimpleListCellRenderer.create { label, value, index -> label.text = value.second })

    if (initialSelectedOpcode != null) {
      selectOpcode(initialSelectedOpcode)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun selectedOpcode(): Int = (selectedItem.castSafelyTo<Pair<String, Int>>())!!.second

  fun selectOpcode(opcode: Int) {
    val comboBoxModel = model as DefaultComboBoxModel
    for (i in 0 until comboBoxModel.size) {
      if (comboBoxModel.getElementAt(i).first == opcode) {
        selectedIndex = i
        return
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
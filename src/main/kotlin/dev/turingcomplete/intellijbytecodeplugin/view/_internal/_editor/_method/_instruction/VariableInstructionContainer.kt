package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTextifierUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.VarInsnNode
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import java.awt.GridBagConstraints
import javax.swing.JLabel

internal class VariableInstructionContainer(varInsnNode: VarInsnNode) : FooInstructionContainer<VarInsnNode>(varInsnNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val opcodesComboBox = OpcodeComboBox(AsmTextifierUtils.variableInstructions, varInsnNode.opcode)
  private val variableField = UiUtils.Field.createNumberField(varInsnNode.`var`)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = UiUtils.Panel.gridBag {
    val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

    add(JLabel("Opcode:"), bag.nextLine().next())
    add(opcodesComboBox, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

    add(JLabel("Local variable:"), bag.nextLine().next().overrideLeftInset(UIUtil.DEFAULT_VGAP / 2))
    add(variableField, bag.next().fillCellHorizontally().weightx(1.0).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideLeftInset(UIUtil.DEFAULT_VGAP / 2))
  }

  override fun resetValue() {
    opcodesComboBox.selectOpcode(instNode.opcode)
    variableField.value = instNode.`var`
  }

  override fun writeValue() {
    instNode.opcode = opcodesComboBox.selectedOpcode()
    instNode.`var` = variableField.value as Int
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
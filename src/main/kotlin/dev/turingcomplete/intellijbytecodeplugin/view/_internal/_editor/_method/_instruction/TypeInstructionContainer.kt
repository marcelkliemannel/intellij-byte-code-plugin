package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTextifierUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.TypeInsnNode
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import java.awt.GridBagConstraints
import javax.swing.JLabel

internal class TypeInstructionContainer(typeInsnNode: TypeInsnNode): FooInstructionContainer<TypeInsnNode>(typeInsnNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val opcodesComboBox = OpcodeComboBox(AsmTextifierUtils.typeInstructions, typeInsnNode.opcode)
  private val descriptorField = JBTextField(typeInsnNode.desc)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = UiUtils.Panel.gridBag {
    val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

    add(JLabel("Opcode:"), bag.nextLine().next())
    add(opcodesComboBox, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

    add(JLabel("Descriptor:"), bag.nextLine().next().overrideLeftInset(UIUtil.DEFAULT_VGAP / 2))
    add(descriptorField, bag.next().fillCellHorizontally().weightx(1.0).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideLeftInset(UIUtil.DEFAULT_VGAP / 2))
  }

  override fun resetValue() {
    opcodesComboBox.selectOpcode(instNode.opcode)
    descriptorField.text = instNode.desc
  }

  override fun writeValue() {
    instNode.opcode = opcodesComboBox.selectedOpcode()
    instNode.desc = descriptorField.text
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
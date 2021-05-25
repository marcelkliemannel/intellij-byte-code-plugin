package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTextifierUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.InsnNode
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import java.awt.GridBagConstraints
import javax.swing.JLabel

internal class InstructionContainer(insnNode: InsnNode): FooInstructionContainer<InsnNode>(insnNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val opcodesComboBox = OpcodeComboBox(AsmTextifierUtils.instructions, insnNode.opcode)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //


  override fun createComponent() = UiUtils.Panel.gridBag {
    val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

    add(JLabel("Opcode:"), bag.nextLine().next())
    add(opcodesComboBox, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))
  }

  override fun resetValue() {
    opcodesComboBox.selectOpcode(instNode.opcode)
  }

  override fun writeValue() {
    // todo
//    instNode.opcode = opcodesComboBox.selectedOpcode()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
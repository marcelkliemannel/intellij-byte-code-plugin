package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor

import com.intellij.icons.AllIcons
import com.intellij.ui.ClickListener
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.*
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction.*
import java.awt.GridBagLayout
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class InstructionsPanel(instructions: InsnList): JPanel(GridBagLayout()) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val instructionContainers: List<FooInstructionContainer<out AbstractInsnNode>>

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val bag = UiUtils.createDefaultGridBag()

    instructionContainers = instructions.mapIndexed { index, instruction ->
      val instructionContainer = when (instruction) {
        is VarInsnNode -> VariableInstructionContainer(instruction)
        is InsnNode -> InstructionContainer(instruction)
        is TypeInsnNode -> TypeInstructionContainer(instruction)
        else -> UnknownInstructionContainer(instruction)
      }

      val topInset = if (index == 0) 0 else UIUtil.DEFAULT_VGAP
      add(JLabel(instruction::class.simpleName), bag.nextLine().next().overrideTopInset(topInset))
      add(instructionContainer.createComponent(), bag.next().overrideTopInset(topInset))
      add(upActionButton(), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(topInset))
      add(downActionButton(), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(topInset))
      add(removeActionButton(), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(topInset))

      instructionContainer
    }.toList()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun downActionButton(): JComponent {
    val label = JLabel(AllIcons.General.ArrowDown)
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (!label.isEnabled) return true

        return true
      }
    }.installOn(label)

    return label
  }

  private fun upActionButton(): JComponent {
    val label = JLabel(AllIcons.General.ArrowUp)
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (!label.isEnabled) return true

        return true
      }
    }.installOn(label)

    return label
  }

  private fun removeActionButton(): JComponent {
    val label = JLabel(AllIcons.General.Remove)
    object : ClickListener() {
      override fun onClick(e: MouseEvent, clickCount: Int): Boolean {
        if (!label.isEnabled) return true

        return true
      }
    }.installOn(label)

    return label
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
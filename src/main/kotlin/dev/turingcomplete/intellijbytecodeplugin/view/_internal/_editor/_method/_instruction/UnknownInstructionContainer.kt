package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import com.intellij.ui.components.JBLabel
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.AbstractInsnNode

internal class UnknownInstructionContainer(isnNode: AbstractInsnNode): FooInstructionContainer<AbstractInsnNode>(isnNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JBLabel("Unknown instruction '${instNode::class.simpleName}'")

  override fun resetValue() {
  }

  override fun writeValue() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
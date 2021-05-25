package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method._instruction

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.AbstractInsnNode
import javax.swing.JComponent

internal abstract class FooInstructionContainer<I: AbstractInsnNode>(val instNode: I) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createComponent(): JComponent

  abstract fun resetValue()

  abstract fun writeValue()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
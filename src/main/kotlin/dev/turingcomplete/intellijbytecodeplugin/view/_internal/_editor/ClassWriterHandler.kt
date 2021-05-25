package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassWriter
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode

abstract class ClassWriterHandler {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun read(classNode: ClassNode)

  abstract fun write(classWriter: ClassWriter);

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
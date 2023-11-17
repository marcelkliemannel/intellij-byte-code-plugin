package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

data class SourceFile internal constructor(val file: VirtualFile, val module: Module) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
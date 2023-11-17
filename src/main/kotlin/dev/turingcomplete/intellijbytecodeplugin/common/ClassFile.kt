package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.vfs.VirtualFile

data class ClassFile internal constructor(val file: VirtualFile, val sourceFile: SourceFile? = null) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    assert(file.extension == "class")
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun refreshValidity(): Boolean {
    file.refresh(false, false)
    return file.isValid
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
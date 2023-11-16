package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

data class ProcessableClassFile(val classFile: VirtualFile, val sourceFile: Pair<VirtualFile, Module>? = null) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    assert(classFile.extension == "class")
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun refreshValidity(): Boolean {
    classFile.refresh(false, false)
    return classFile.isValid
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
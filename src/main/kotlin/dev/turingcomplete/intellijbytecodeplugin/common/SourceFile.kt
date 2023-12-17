package dev.turingcomplete.intellijbytecodeplugin.common

import com.google.common.base.Objects
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

sealed class SourceFile private constructor(val file: VirtualFile) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is SourceFile) return false

    if (file != other.file) return false

    return true
  }

  override fun hashCode(): Int {
    return file.hashCode()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class CompilableSourceFile(file: VirtualFile, val module: Module) : SourceFile(file) {

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as CompilableSourceFile

      return file == other.file && module == other.module
    }

    override fun hashCode(): Int {
      return Objects.hashCode(module, file)
    }

    override fun toString(): String = "CompilableSourceFile(file=$file, module=$module)"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class NonCompilableSourceFile(file: VirtualFile) : SourceFile(file) {

    override fun toString(): String = "NonCompilableSourceFile(file=$file)"
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}
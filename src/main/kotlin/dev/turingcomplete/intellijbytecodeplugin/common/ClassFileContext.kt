package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode

interface ClassFileContext {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  /**
   * Working in a synchronous way is for testing.
   */
  fun workAsync(): Boolean

  fun project(): Project

  fun classFile(): VirtualFile
  
  fun sourceFile(): SourceFile?

  fun classNode(): ClassNode

  fun classReader(): ClassReader

  fun relatedClassFiles(): List<VirtualFile>

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class VerificationResult(val success: Boolean, val output: String)
}
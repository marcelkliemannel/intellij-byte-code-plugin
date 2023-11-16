package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode

internal class DefaultClassFileContext(
  private val project: Project,
  private val classFile: VirtualFile,
  private val workAsync: Boolean
) : ClassFileContext {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val ASM_API: Int = Opcodes.ASM9
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val classReader: ClassReader = ClassReader(classFile.inputStream)
  private val classNode: ClassNode = readClassNode()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun workAsync(): Boolean = workAsync

  override fun classFile(): VirtualFile = classFile

  override fun project(): Project = project

  override fun classNode(): ClassNode = classNode

  override fun classReader(): ClassReader = classReader

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun readClassNode() = ClassNode(ASM_API).apply {
    classReader.accept(this, ClassReader.EXPAND_FRAMES)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
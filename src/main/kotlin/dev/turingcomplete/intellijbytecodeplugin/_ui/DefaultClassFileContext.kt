package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ProcessableClassFile
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode

internal class DefaultClassFileContext(
  private val project: Project,
  private val processableClassFile: ProcessableClassFile,
  private val workAsync: Boolean
) : ClassFileContext {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val ASM_API: Int = Opcodes.ASM9
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val classReader: ClassReader = ClassReader(processableClassFile.classFile.inputStream)
  private val classNode: ClassNode = readClassNode()
  private val nestedClassFiles: List<VirtualFile> = findRelatedClassFiles()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun workAsync(): Boolean = workAsync

  override fun classFile(): VirtualFile = processableClassFile.classFile

  override fun sourceFile(): SourceFile? = processableClassFile.sourceFile

  override fun project(): Project = project

  override fun classNode(): ClassNode = classNode

  override fun classReader(): ClassReader = classReader
  
  override fun relatedClassFiles(): List<VirtualFile> = nestedClassFiles

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun readClassNode() = ClassNode(ASM_API).apply {
    classReader.accept(this, ClassReader.EXPAND_FRAMES)
  }

  private fun findRelatedClassFiles(): List<VirtualFile> {
    val parentDirectory = processableClassFile.classFile.parent
    if (!parentDirectory.isDirectory) {
      return emptyList()
    }

    val rootClassFileNamePrefix = processableClassFile.classFile.nameWithoutExtension.takeWhile { it != '$' }
    return parentDirectory.children
      .filter { it.extension == "class" && it.name.startsWith(rootClassFileNamePrefix) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
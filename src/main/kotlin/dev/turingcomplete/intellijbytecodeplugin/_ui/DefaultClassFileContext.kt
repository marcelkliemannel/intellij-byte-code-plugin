package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode

internal class DefaultClassFileContext(
  private val project: Project,
  private val classFile: ClassFile,
  private val workAsync: Boolean,
) : ClassFileContext {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    const val ASM_API: Int = Opcodes.ASM9
  }

  // -- Properties ---------------------------------------------------------- //

  private val classReader: ClassReader = ClassReader(classFile.file.inputStream)
  private val classNode: ClassNode = readClassNode()
  private val relatedClassFiles: List<VirtualFile> = findRelatedClassFiles()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun workAsync(): Boolean = workAsync

  override fun classFile(): ClassFile = classFile

  override fun project(): Project = project

  override fun classNode(): ClassNode = classNode

  override fun classReader(): ClassReader = classReader

  override fun relatedClassFiles(): List<VirtualFile> = relatedClassFiles

  // -- Private Methods ----------------------------------------------------- //

  private fun readClassNode() =
    ClassNode(ASM_API).apply { classReader.accept(this, ClassReader.EXPAND_FRAMES) }

  private fun findRelatedClassFiles(): List<VirtualFile> {
    val parentDirectory = classFile.file.parent
    if (!parentDirectory.isDirectory) {
      return emptyList()
    }

    val rootClassFileName = classFile.file.nameWithoutExtension.takeWhile { it != '$' }
    val rootClassFileNamePrefix = "$rootClassFileName$"
    return parentDirectory.children.filter {
      it.extension == "class" &&
        (it.name == "$rootClassFileName.class" || it.name.startsWith(rootClassFileNamePrefix))
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
}

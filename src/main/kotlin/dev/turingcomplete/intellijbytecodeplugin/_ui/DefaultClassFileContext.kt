package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import org.jetbrains.annotations.TestOnly


class DefaultClassFileContext private constructor(private val project: Project,
                                                  private val classFile: VirtualFile,
                                                  private val workAsync: Boolean)
  : ClassFileContext {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val ASM_API: Int = Opcodes.ASM9

    fun loadAsync(project: Project,
                  classFile: VirtualFile,
                  onSuccess: (ClassFileContext) -> Unit,
                  onError: (Throwable) -> Unit) {

      dev.turingcomplete.intellijbytecodeplugin._other.AsyncUtils.runAsync(project, { DefaultClassFileContext(project, classFile, true) }, onSuccess, onError)
    }

    @TestOnly
    fun loadSync(project: Project,
                 classFile: VirtualFile,
                 onSuccess: (ClassFileContext) -> Unit,
                 onError: (Throwable) -> Unit) {

      try {
        onSuccess(DefaultClassFileContext(project, classFile, false))
      }
      catch (e: Throwable) {
        onError(e)
      }
    }
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
package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction

class DecompileByteCodeAction : ByteCodeAction("Decompile Class File", null, AllIcons.Actions.Compile) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    val project = CommonDataKeys.PROJECT.getData(e.dataContext) ?: throw IllegalStateException("snh: Missing data: ${CommonDataKeys.PROJECT.name}")

    val classFileContext = dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys.CLASS_FILE_CONTEXT_DATA_KEY.getData(e.dataContext)
                           ?: throw IllegalStateException("snh: Missing data: ${dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys.CLASS_FILE_CONTEXT_DATA_KEY.name}")

    FileEditorManager.getInstance(project).openEditor(OpenFileDescriptor(project, classFileContext.classFile()), true)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
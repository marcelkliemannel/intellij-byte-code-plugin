package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import dev.turingcomplete.intellijbytecodeplugin.common.ByteCodeAnalyserOpenClassFileService
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesToolWindowAction

internal class CurrentEditorFileAction : OpenClassFilesToolWindowAction(
  "Analyze Current Editor File",
  "Analyze current editor file"
) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun execute(project: Project) {
    val editorPsiFiles = FileEditorManager.getInstance(project).selectedEditors.mapNotNull { editor ->
      if (editor is TextEditor) {
        PsiDocumentManager.getInstance(project).getPsiFile(editor.editor.document)
      }
      else {
        null
      }
    }.toList()
    project.getService(ByteCodeAnalyserOpenClassFileService::class.java).openPsiFiles(editorPsiFiles)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin.openclassfiles

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class OpenClassFilesToolWindowAction(val title: String, val icon: Icon? = null) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<OpenClassFilesToolWindowAction> = ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.openClassFilesAction")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createAsEmbeddedAction(project: Project) = object: DumbAwareAction(title, null, icon) {
    override fun actionPerformed(e: AnActionEvent) {
      execute(project)
    }
  }

  abstract fun execute(project: Project)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
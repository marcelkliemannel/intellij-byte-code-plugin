package dev.turingcomplete.intellijbytecodeplugin.openclassfiles

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon
import org.jetbrains.annotations.Nls

abstract class OpenClassFilesToolWindowAction(
  @NlsActions.ActionText val actionTitle: String,
  @Nls val linkTitle: String,
  val icon: Icon? = null,
) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<OpenClassFilesToolWindowAction> =
      ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.openClassFilesAction")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun createAsEmbeddedAction(project: Project) =
    object : DumbAwareAction(actionTitle, null, icon) {
      override fun actionPerformed(e: AnActionEvent) {
        execute(project)
      }
    }

  abstract fun execute(project: Project)

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

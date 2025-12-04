package dev.turingcomplete.intellijbytecodeplugin.tool

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class ByteCodeTool(val title: String, val icon: Icon? = null) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<ByteCodeTool> =
      ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.byteCodeTool")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  abstract fun execute(project: Project?)

  fun toAction(): DumbAwareAction {
    return object : DumbAwareAction(title, null, icon) {
      override fun actionPerformed(e: AnActionEvent) {
        execute(CommonDataKeys.PROJECT.getData(e.dataContext))
      }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

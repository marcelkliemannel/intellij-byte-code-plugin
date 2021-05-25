package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor._method

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import javax.swing.JComponent

class ModifyMethodDialog(project: Project?, methodNode: MethodNode) : DialogWrapper(project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterPanel(): JComponent? {
    TODO("Not yet implemented")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
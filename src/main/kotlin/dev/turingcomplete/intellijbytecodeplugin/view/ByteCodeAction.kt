@file:Suppress("MemberVisibilityCanBePrivate")

package dev.turingcomplete.intellijbytecodeplugin.view

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions
import javax.swing.Icon

abstract class ByteCodeAction(@NlsActions.ActionText text : String?,
                              @NlsActions.ActionDescription description : String?,
                              icon : Icon?) : DumbAwareAction(text, description, icon) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<ByteCodeAction> = ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.byteCodeAction")

    fun DefaultActionGroup.addAllByteCodeActions() {
      EP.extensions.takeIf {it.isNotEmpty() }?.let {
        addAll(it.toList())
      }
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

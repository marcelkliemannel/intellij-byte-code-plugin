package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.ui.components.JBLabel
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

internal open class TextNode(title: String, icon: Icon? = null) : StructureNode() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val component: JComponent by lazy { JBLabel(title, icon, SwingConstants.LEFT) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun component(selected: Boolean, context: StructureTreeContext, componentValid: Boolean) = component

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
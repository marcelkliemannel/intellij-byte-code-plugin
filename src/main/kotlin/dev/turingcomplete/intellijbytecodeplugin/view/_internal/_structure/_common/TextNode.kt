package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.ui.components.JBLabel
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

internal open class TextNode(private val text: String, icon: Icon? = null) : StructureNode() {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private val component: JComponent by lazy { JBLabel(text, icon, SwingConstants.LEFT) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun component(
    selected: Boolean,
    context: StructureTreeContext,
    componentValid: Boolean,
  ) = component

  override fun searchText(context: StructureTreeContext) = text

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

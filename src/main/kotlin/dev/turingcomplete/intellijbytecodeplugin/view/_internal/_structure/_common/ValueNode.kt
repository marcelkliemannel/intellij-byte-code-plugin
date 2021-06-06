package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.ui.components.JBLabel
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.SearchProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

internal open class ValueNode(protected val preFix: String? = null,
                              val displayValue: (StructureTreeContext) -> String,
                              val rawValue: (StructureTreeContext) -> String = displayValue,
                              protected val postFix: String? = null,
                              protected val icon: Icon? = null,
                              searchProvider: SearchProvider? = null) : StructureNode(searchProvider) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var component: JComponent? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  constructor(preFix: String? = null, displayValue: String, rawValue: String = displayValue, postFix: String? = null, icon: Icon? = null)
          : this(preFix, { displayValue }, { rawValue }, postFix, icon)

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun component(selected: Boolean, context: StructureTreeContext, componentValid: Boolean): JComponent {
    if (!componentValid) {
      val text = StringBuilder().apply {
        preFix?.let { append(it).append(" ") }
        append(displayValue(context))
        postFix?.let { append(" ").append(it) }
      }
      component = JBLabel("<html>$text</html>", icon, SwingConstants.LEFT)
    }

    return component!!
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
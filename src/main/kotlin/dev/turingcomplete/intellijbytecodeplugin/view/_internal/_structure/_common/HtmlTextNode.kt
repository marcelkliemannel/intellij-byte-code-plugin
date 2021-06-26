package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.SwingConstants

internal class HtmlTextNode(preFix: String? = null,
                            displayValue: (StructureTreeContext) -> String,
                            rawValue: (StructureTreeContext) -> String = displayValue,
                            postFix: String? = null,
                            icon: Icon? = null,
                            goToProvider: GoToProvider? = null)
  : ValueNode(preFix, displayValue, rawValue, postFix, icon, goToProvider) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val contextHelpFont = JBUI.Fonts.smallFont()
    private val contextHelpFontCss = "font-family: '${contextHelpFont.family}'; font-size: ${contextHelpFont.size}pt;"
    private val notSelectedCss = ".contextHelp { color: #${ColorUtil.toHex(UIUtil.getContextHelpForeground())}; margin-left: 50pt; $contextHelpFontCss }"
    private val selectedCss = ".contextHelp { color: #${ColorUtil.toHex(UIUtil.getListForeground(true, true))}; margin-left: 50pt; $contextHelpFontCss }"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var notSelectedComponent: JComponent? = null
  private var selectedComponent: JComponent? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  constructor(preFix: String? = null,
              displayValue: String,
              rawValue: String = displayValue,
              postFix: String? = null,
              icon: Icon? = null) : this(preFix, { displayValue }, { rawValue }, postFix, icon)

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun component(selected: Boolean, context: StructureTreeContext, componentValid: Boolean): JComponent {
    if (!componentValid) {
      notSelectedComponent = createComponent(notSelectedCss, context)
      selectedComponent = createComponent(selectedCss, context)
    }

    return if (selected) selectedComponent!! else notSelectedComponent!!
  }

  override fun searchText(context: StructureTreeContext) = rawValue(context)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createComponent(css: String, context: StructureTreeContext): JComponent {
    val text = HtmlBuilder()
            .append(HtmlChunk.raw(css).wrapWith("style").attr("type", "text/css").wrapWith("head"))
            .append(HtmlChunk.raw(StringBuilder().apply {
              preFix?.let { append(it).append(" ") }
              append(displayValue(context))
              postFix?.let { append(" ").append(it) }
            }.toString().replace(" ", "&nbsp;")).wrapWith("body"))
            .wrapWith("html")
            .toString()

    return JBLabel(text, icon, SwingConstants.LEFT)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
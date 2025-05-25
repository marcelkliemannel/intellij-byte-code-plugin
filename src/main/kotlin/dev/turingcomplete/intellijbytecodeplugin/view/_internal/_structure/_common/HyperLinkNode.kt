package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

internal open class HyperLinkNode(
  private val text: String,
  initialHyperLinkListener: HyperLinkListener? = null,
) : StructureNode(), InteractiveNode {

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private val hyperLinkListeners = mutableSetOf<HyperLinkListener>()
  private var component: JComponent? = null

  // -- Initialization ------------------------------------------------------ //

  init {
    initialHyperLinkListener?.let { hyperLinkListeners.add(it) }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun component(
    selected: Boolean,
    context: StructureTreeContext,
    componentValid: Boolean,
  ): JComponent {
    if (component == null) {
      // Without the wrapping later component adjustments, like borders, will
      // have no effects.
      component =
        JBUI.Panels.simplePanel(
          HyperlinkLabel(text).apply {
            hyperLinkListeners.forEach { hyperLinkListener ->
              addHyperlinkListener { event -> hyperLinkListener.handle(event, context) }
            }
          }
        )
    }

    return component!!
  }

  override fun searchText(context: StructureTreeContext): Nothing? = null

  fun addHyperLinkListener(hyperLinkListener: HyperLinkListener) {
    hyperLinkListeners.add(hyperLinkListener)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  fun interface HyperLinkListener {
    fun handle(event: HyperlinkEvent, context: StructureTreeContext)
  }
}

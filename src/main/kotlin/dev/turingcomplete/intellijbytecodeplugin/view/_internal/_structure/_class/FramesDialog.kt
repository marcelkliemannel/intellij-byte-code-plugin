package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.copyable
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideBottomInset
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodFramesUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

class FramesDialog(private val methodNode: MethodNode,
                   private val initialTypeNameRenderMode: TypeUtils.TypeNameRenderMode,
                   private val methodFrames: List<MethodFramesUtils.MethodFrame>,
                   project: Project?) : DialogWrapper(project) {

// -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    this.title = "Frames of Method '${methodNode.name}'"
    setSize(700, 300)
    isModal = false
    init()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterPanel(): JComponent {
    return BorderLayoutPanel().apply {
      addToCenter(FramesPanel(initialTypeNameRenderMode, methodFrames).apply {
        border = BorderFactory.createLineBorder(JBColor.border())
      })
    }
  }

  override fun createActions() = arrayOf(myOKAction)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
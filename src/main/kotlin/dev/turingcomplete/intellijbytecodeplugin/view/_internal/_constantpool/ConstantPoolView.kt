package dev.turingcomplete.intellijbytecodeplugin.view._internal._constantpool

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPool
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import javax.swing.JComponent
import javax.swing.SwingConstants

class ConstantPoolView(classFileContext: ClassFileContext)
  : ByteCodeView(classFileContext, "Constant Pool", AllIcons.Nodes.Constant), DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val centerComponent : SimpleToolWindowPanel by lazy { SimpleToolWindowPanel(true, false) }

  private var constantPoolTable: ConstantPoolTable? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterComponent(): JComponent {
    return centerComponent.apply {
      toolbar = createToolbar(this)

      asyncReadConstantPool()
    }
  }

  override fun reParseClassNodeContext() {
    asyncReadConstantPool()
  }

  private fun asyncReadConstantPool() {
    constantPoolTable = null
    centerComponent. setContent(JBLabel("Parsing constant pool...", AnimatedIcon.Default(), SwingConstants.CENTER))

    val onSuccess: (ConstantPool) -> Unit = { constantPool ->
      ApplicationManager.getApplication().invokeLater {
        constantPoolTable = ConstantPoolTable(constantPool)
        centerComponent.setContent(ScrollPaneFactory.createScrollPane(constantPoolTable, true))
      }
    }
    AsyncUtils.runAsync(classFileContext.project(), { ConstantPool.create(classFileContext.classFile()) },
                        onSuccess, { cause -> onError("Failed to parse constant pool", cause) })
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(targetComponent: JComponent): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      addAllByteCodeActions()

      addSeparator()

      add(createResolveIndicesAction())
    }
    return ActionManager.getInstance().createActionToolbar("${ByteCodeToolWindowFactory.TOOLBAR_PLACE_PREFIX}.constantPoolView", toolbarGroup, true).run {
      setTargetComponent(targetComponent)
      component
    }
  }

  private fun createResolveIndicesAction(): ToggleAction {
    return object : DumbAwareToggleAction("Resolve Referenced Indices in Values", null, AllIcons.Diff.MagicResolveToolbar) {

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = constantPoolTable != null
      }

      override fun isSelected(e: AnActionEvent): Boolean = constantPoolTable?.resolveIndices ?: false

      override fun setSelected(e: AnActionEvent, state: Boolean) {
        constantPoolTable?.let { it.resolveIndices = state }
      }

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = ConstantPoolView(classFileContext)
  }
}
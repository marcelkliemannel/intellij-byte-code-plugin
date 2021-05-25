package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.icons.AllIcons
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class.ClassStructureNode
import javax.swing.JComponent

class StructureView(classFileContext: ClassFileContext)
  : ByteCodeView(classFileContext, "Structure", AllIcons.Toolwindows.ToolWindowStructure) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val tree: StructureTree by lazy { StructureTree.create(classFileContext, this) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterComponent(): JComponent {
    return SimpleToolWindowPanel(true, false).apply {
      toolbar = createToolbar()
      setContent(ScrollPaneFactory.createScrollPane(tree, false))
    }
  }

  override fun retry() {
    createRootNode(classFileContext)
    tree.reload()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createRootNode(classFileContext: ClassFileContext) : ClassStructureNode {
    return ClassStructureNode(classFileContext.classNode(), classFileContext.classFile())
  }

  private fun createToolbar(): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      addAll(tree.createToolBarActions())

      addSeparator()

      val treeExpander = DefaultTreeExpander(tree)
      add(ExpandAllAction { treeExpander })
      add(CollapseAllAction { treeExpander })

      addAllByteCodeActions()
    }
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarGroup, true).component
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = StructureView(classFileContext)
  }
}

package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.icons.AllIcons
import com.intellij.ide.DefaultTreeExpander
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import javax.swing.JComponent

internal class StructureView(classFileContext: ClassFileContext)
  : ByteCodeView(classFileContext, "Structure", AllIcons.Toolwindows.ToolWindowStructure), DataProvider {

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
    tree.reload()
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.`is`(dataId) -> classFileContext.classFile()
      else -> super.getData(dataId)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

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

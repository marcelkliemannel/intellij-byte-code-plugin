package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.DropDownLink
import com.intellij.util.asSafely
import com.intellij.util.ui.GridBag
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin._ui.SimpleListCellRenderer
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.withCommonsDefaults
import dev.turingcomplete.intellijbytecodeplugin.common.ByteCodeAnalyserOpenClassFileService
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

internal class StructureView(classFileContext: ClassFileContext)
  : ByteCodeView(classFileContext, "Structure"), DataProvider {

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private val tree: StructureTree by lazy { StructureTree(classFileContext, this) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun createCenterComponent(): JComponent {
    return SimpleToolWindowPanel(true, false).apply {
      toolbar = createToolbar()
      setContent(ScrollPaneFactory.createScrollPane(tree, false))
    }
  }

  override fun reParseClassNodeContext() {
    tree.reload()
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.`is`(dataId) -> classFileContext.classFile()
      else -> super.getData(dataId)
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createToolbar(): JComponent = JPanel(GridBagLayout()).apply {
    val bag = GridBag().withCommonsDefaults().setDefaultAnchor(GridBagConstraints.WEST)
    add(createToolbarActionsComponent(this), bag.nextLine().next().fillCellHorizontally().weightx(1.0))

    createOpenNestedClassLink()?.let {
      add(it, bag.next().fillCellHorizontally().overrideLeftInset(2).overrideLeftInset(2))
    }
  }

  private fun createToolbarActionsComponent(targetComponent: JComponent): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      addAllByteCodeActions()

      addSeparator()

      addAll(tree.createToolBarActions())
    }
    return ActionManager.getInstance().createActionToolbar("${ByteCodeToolWindowFactory.TOOLBAR_PLACE_PREFIX}.structureView", toolbarGroup, true).run {
      setTargetComponent(targetComponent)
      component
    }
  }

  private fun createOpenNestedClassLink(): JComponent? {
    val relatedClassFilesToTitle = classFileContext.relatedClassFiles().map { it to it.nameWithoutExtension }
    if (relatedClassFilesToTitle.size <= 1) {
      return null
    }

    val createPopUp: (DropDownLink<Pair<VirtualFile?, String>>) -> JBPopup = {
      JBPopupFactory.getInstance()
        .createPopupChooserBuilder(relatedClassFilesToTitle)
        .setRenderer(SimpleListCellRenderer { it.asSafely<Pair<VirtualFile?, String>>()?.second ?: "" })
        .setItemChosenCallback {
          classFileContext.project().getService(ByteCodeAnalyserOpenClassFileService::class.java)
            .openClassFiles(listOf(ClassFile(it.first, classFileContext.classFile().sourceFile)))
        }
        .createPopup()
    }
    return object : DropDownLink<Pair<VirtualFile?, String>>(Pair(null, "Open related class file"), createPopUp) {
      override fun itemToString(item: Pair<VirtualFile?, String>) = item.second
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = StructureView(classFileContext)
  }
}

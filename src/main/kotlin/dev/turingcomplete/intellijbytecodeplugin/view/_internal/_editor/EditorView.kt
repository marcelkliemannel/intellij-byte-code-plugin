package dev.turingcomplete.intellijbytecodeplugin.view._internal._editor

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmClassVersionUtils
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmMethodUtils
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTypeUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

class EditorView(classFileContext: ClassFileContext) : ByteCodeView(classFileContext, "Editor", AllIcons.Actions.Edit) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createCenterComponent(): JComponent {
    return UiUtils.Panel.scroll(JPanel(GridBagLayout()).apply {
      border = JBEmptyBorder(UIUtil.getRegularPanelInsets())

      val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

      val classNode = classFileContext.classNode()

      listOf(EditorField.Selection("Class Version:",
                                   AsmClassVersionUtils.CLASS_VERSIONS,
                                   { AsmClassVersionUtils.toClassVersion(classNode.version) },
                                   { classNode.version = it.major.toInt() }), // todo if classversion is out of range add is as unknown
             EditorField.Text("Name:", { classNode.name }, { classNode.name = it }),
             EditorField.Number("Access:", { classNode.access }, { classNode.access = it }),
             EditorField.Text("Signature:", { classNode.signature }, { classNode.signature = it }),
             EditorField.Text("Super name:", { classNode.superName }, { classNode.superName = it }),
             EditorField.ItemsList("Interfaces", { classNode.interfaces }, { classNode.interfaces = it }),
             EditorField.ItemsList("Methods", { classNode.methods }, { classNode.methods = it }, { AsmMethodUtils.toReadableDeclaration(it.name, it.desc, classNode.name, AsmTypeUtils.TypeNameRenderMode.INTERNAL, AsmMethodUtils.MethodDescriptorRenderMode.DESCRIPTOR, true) })
            ).forEach { editorField ->
        val hasPreText = editorField.preText != null
        if (hasPreText) {
          add(JLabel(editorField.preText), bag.nextLine().next())
          add(editorField.createFieldComponent(), bag.next().weightx(1.0).fillCellHorizontally().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))
        }
        else {
          add(editorField.createFieldComponent(), bag.nextLine().next().weightx(1.0).coverLine().fillCellHorizontally())
        }
      }

      add(JBCheckBox("Verify before save"), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP))

      add(JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP)).apply {
        add(JButton("<html>Save</html>", AllIcons.Actions.MenuSaveall))
        add(JButton("Save As...", AllIcons.Actions.MenuSaveall))
      }, bag.nextLine().next().coverLine().overrideTopInset(2))

      add(JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP)).apply {
        add(JButton("Verify"))
        add(JButton("Compare with original", AllIcons.Actions.Diff))
      }, bag.nextLine().coverLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    }, hsbPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
  }

  override fun retry() {
    TODO("Not yet implemented")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = EditorView(classFileContext)
  }
}
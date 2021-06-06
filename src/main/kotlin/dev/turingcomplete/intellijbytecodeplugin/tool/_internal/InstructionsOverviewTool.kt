package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode

@Deprecated(message = "Not fully implemented yet. May be added in future versions.")
internal class InstructionsOverviewTool : ByteCodeTool("Instructions Overview") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun execute(project: Project?) {

    val root = OpcodeNode("")
    val addGroupToRoot : (String, Array<Pair<Int, String>>) -> Unit = { title, instructions ->
      val groupNode = OpcodeNode(title)
      instructions.forEach { groupNode.add(OpcodeNode(it)) }
      root.add(groupNode)
    }
    /*
    addGroupToRoot("Simple instructions", AsmTextifierUtils.SIMPLE_INSTRUCTIONS)
    addGroupToRoot("Jump instructions", AsmTextifierUtils.JUMP_INSTRUCTIONS)
    addGroupToRoot("Variable instructions", AsmTextifierUtils.VARIABLE_INSTRUCTIONS)
    addGroupToRoot("Integer instructions", AsmTextifierUtils.INTEGER_INSTRUCTIONS)
    addGroupToRoot("Type instructions", AsmTextifierUtils.TYPE_INSTRUCTIONS)
    addGroupToRoot("Method instructions", AsmTextifierUtils.METHOD_INSTRUCTIONS)
    addGroupToRoot("Field instructions", AsmTextifierUtils.FIELD_INSTRUCTIONS)
    root.add(OpcodeNode(AsmTextifierUtils.LDC_INSTRUCTION))
    root.add(OpcodeNode(AsmTextifierUtils.IINC_INSTRUCTION))
    root.add(OpcodeNode(AsmTextifierUtils.TABLE_SWITCH_INSTRUCTION))
    root.add(OpcodeNode(AsmTextifierUtils.LOOK_UP_SWITCH__INSTRUCTION))
    root.add(OpcodeNode(AsmTextifierUtils.INVOKE_DYNAMIC_INSTRUCTION))
    root.add(OpcodeNode(AsmTextifierUtils.MULTI_NEW_ARRAY_INSTRUCTION))*/

    val treeTable = TreeTable(ListTreeTableModel(root, arrayOf(InstructionColumnInfo(), OpcodeColumnInfo()))).apply {
      setRootVisible(false)
      tree.showsRootHandles = true
    }

    UiUtils.Dialog.show("Instructions Overview", ScrollPaneFactory.createScrollPane(JPanel(GridBagLayout()).apply {
      val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)
      add(ScrollPaneFactory.createScrollPane(treeTable), bag.nextLine().next().coverLine().fillCell().weightx(1.0).weighty(1.0).overrideTopInset(UIUtil.LARGE_VGAP))
    }, true), Dimension(500, 550), project)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpcodeNode(val textified: String, val opcode: Int? = null) : DefaultMutableTreeNode(textified) {

    constructor(pair: Pair<Int, String>) : this(pair.second, pair.first)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class InstructionColumnInfo : ColumnInfo<OpcodeNode, String>("Instruction") {

    override fun valueOf(item: OpcodeNode): String = item.textified

    override fun getColumnClass(): Class<*> = TreeTableModel::class.java
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpcodeColumnInfo : ColumnInfo<OpcodeNode, String>("Opcode") {

    override fun valueOf(item: OpcodeNode): String = item.opcode?.toString() ?: ""
  }
}
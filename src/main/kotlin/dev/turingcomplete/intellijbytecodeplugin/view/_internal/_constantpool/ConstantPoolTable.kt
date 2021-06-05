package dev.turingcomplete.intellijbytecodeplugin.view._internal._constantpool

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.PlatformIcons
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.configureForCell
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPool
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPoolInfo
import java.awt.Component
import java.awt.Rectangle
import java.awt.datatransfer.StringSelection
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import kotlin.properties.Delegates

internal class ConstantPoolTable(private val constantPool: ConstantPool) : JBTable() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var resolveIndices: Boolean by Delegates.observable(false) { _, old, new ->
    if (old != new) {
      revalidate()
      repaint()
    }
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    model = MyTableModel()

    columnModel.apply {
      getColumn(0).apply {
        preferredWidth = 50
        minWidth = preferredWidth
      }

      getColumn(1).apply {
        preferredWidth = 100
        minWidth = preferredWidth
      }

      getColumn(2).apply {
        preferredWidth = Int.MAX_VALUE
        cellRenderer = ConstantPoolInfoValueTableCellRenderer()
      }
    }

    addMouseListener(MyMouseAdapter())
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun toDisplayText(value: ConstantPoolInfo): String {
    return if (resolveIndices) value.resolvedDisplayText(constantPool) else value.unresolvedDisplayText
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class MyTableModel : DefaultTableModel() {

    override fun getRowCount(): Int {
      return constantPool.entries.size
    }

    override fun getColumnCount() = 3

    override fun getColumnName(column: Int) = when (column) {
      0 -> "Index"
      1 -> "Type"
      2 -> "Value"
      else -> throw IllegalArgumentException("snh: Unknown column: $column")
    }

    override fun getValueAt(row: Int, column: Int): Any = when (column) {
      0 -> row + 1
      1 -> constantPool.entries[row].type
      2 -> constantPool.entries[row]
      else -> throw IllegalArgumentException("snh: Unknown column: $column")
    }

    override fun isCellEditable(row: Int, column: Int) = false
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class ConstantPoolInfoValueTableCellRenderer : JBLabel(), TableCellRenderer {

    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
      value as ConstantPoolInfo
      text = toDisplayText(value)
      configureForCell(table, isSelected, hasFocus)
      return this
    }
  }
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class MyMouseAdapter : MouseAdapter() {

    override fun mousePressed(e: MouseEvent) {
      handleTreeMouseEvent(e)
    }

    override fun mouseReleased(e: MouseEvent) {
      handleTreeMouseEvent(e)
    }

    private fun handleTreeMouseEvent(event: InputEvent) {
      if (event !is MouseEvent || !event.isPopupTrigger) {
        return
      }

      val selectedRow = this@ConstantPoolTable.selectedRow
      if (selectedRow >= 0 && this@ConstantPoolTable.selectedRowCount == 1) {
        val constantPoolInfo = constantPool.entries[selectedRow]
        val actions = DefaultActionGroup().apply {
          // Go to indices
          val selectTableRow: (Int) -> Unit = { row ->
            this@ConstantPoolTable.setRowSelectionInterval(row, row)
            scrollRectToVisible(Rectangle(this@ConstantPoolTable.getCellRect(row, 0, true)));
          }
          constantPoolInfo.goToIndices.forEach { goToIndex -> add(GoToIndexAction(goToIndex, selectTableRow)) }

          // Copy & view action
          val value = toDisplayText(constantPoolInfo)
          if (value.isNotBlank()) {
            if (constantPoolInfo.goToIndices.isNotEmpty()) {
              addSeparator()
            }

            add(ConstantPoolInfoCopyValueAction(value))
            add(ConstantPoolInfoViewValueAction(value))
          }
        }
        ActionManager.getInstance()
                .createActionPopupMenu(ActionPlaces.UNKNOWN, actions)
                .component
                .show(event.getComponent(), event.x, event.y)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ConstantPoolInfoCopyValueAction(private val value: String)
    : DumbAwareAction("Copy Value", null, PlatformIcons.COPY_ICON) {

    override fun actionPerformed(e: AnActionEvent) {
      CopyPasteManager.getInstance().setContents(StringSelection(value))
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ConstantPoolInfoViewValueAction(private val value: String) : DumbAwareAction("View Value") {

    override fun actionPerformed(e: AnActionEvent) {
      UiUtils.PopUp.showTextAreaPopup(value, e.dataContext)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class GoToIndexAction(private val index: Int, private val selectTableRow: (Int) -> Unit)
    : DumbAwareAction("Go To Index $index") {

    override fun actionPerformed(e: AnActionEvent) {
      selectTableRow(index - 1)
    }
  }
}
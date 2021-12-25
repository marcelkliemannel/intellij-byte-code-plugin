package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin._ui.copyable
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import java.awt.Dimension
import java.util.*
import javax.swing.JLabel
import javax.swing.table.DefaultTableModel

internal class AccessCalculatorPanel(private val accessGroup: AccessGroup) : BorderLayoutPanel() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val result = JBLabel("Result:").apply {
    border = JBEmptyBorder(0, 0, 2, 0)
  }.copyable()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    addToTop(JLabel(accessGroup.toString()).apply { border = JBEmptyBorder(2, 0, 0, 0) })
    addToCenter(ScrollPaneFactory.createScrollPane(AccessCalculatorTable()))
    addToBottom(result)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class AccessCalculatorTable : JBTable(AccessCalculatorTableModel()) {

    init {
      minimumSize = Dimension(200, 80)

      columnModel.apply {
        getColumn(0).apply {
          preferredWidth = 30
          minWidth = 30
        }
        getColumn(1).apply {
          preferredWidth = 100
          minWidth = 100
        }
        getColumn(2).apply {
          preferredWidth = 70
          minWidth = 70
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class AccessCalculatorTableModel : DefaultTableModel() {

    // [0: Selected; 1: Name; 2: Value]
    private val data : List<Array<Any>> = accessGroup.accesses.map { arrayOf(false, it.name.lowercase(Locale.getDefault()), it.value) }

    init {
      updateResult()
    }

    override fun getRowCount(): Int = accessGroup.accesses.size

    override fun getColumnCount(): Int = 3

    override fun getColumnName(columnIndex: Int) = when(columnIndex) {
      0 -> ""
      1 -> "Name"
      2 -> "Value"
      else -> throw IllegalArgumentException("Unknown column index: $columnIndex")
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int) = data[rowIndex][columnIndex]

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == 0

    override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
      if (columnIndex != 0) {
        throw IllegalArgumentException("Column index $columnIndex is not editable.")
      }

      data[rowIndex][0] = value
      updateResult()
    }

    override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      else -> String::class.java
    }

    private fun updateResult() {
      val resultValue = data.filter { it[0] == true }.map { it[2] as Int }.reduceOrNull { a, b -> a.or(b) } ?: 0
      result.text = "Result: $resultValue"
    }
  }
}
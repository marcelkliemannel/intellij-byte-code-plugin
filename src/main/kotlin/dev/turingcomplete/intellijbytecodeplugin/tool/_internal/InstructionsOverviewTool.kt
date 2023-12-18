package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.CopyValueAction
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils.Table.getSingleSelectedValue
import dev.turingcomplete.intellijbytecodeplugin._ui.ViewValueAction
import dev.turingcomplete.intellijbytecodeplugin._ui.getMaxRowWith
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin._ui.withCommonsDefaults
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.InputStreamReader
import java.util.*
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

class InstructionsOverviewTool : ByteCodeTool("Instructions Overview") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val COLUMNS = Vector(listOf("Instruction", "Opcode", "Operands", "Stack modification", "Description"))

    private val OPCODE_COMPARATOR = Comparator<String> { a, b -> a.toInt().compareTo(b.toInt()) }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun execute(project: Project?) {
    if (project?.isDisposed == true) {
      return
    }

    UiUtils.Dialog.show("Instructions Overview", JPanel(GridBagLayout()).apply {
      val bag = GridBag().withCommonsDefaults().setDefaultAnchor(GridBagConstraints.WEST)

      val table = ScrollPaneFactory.createScrollPane(InstructionsTable())
      add(table, bag.nextLine().next().coverLine().fillCell().weightx(1.0).weighty(1.0))

      add(JLabel("Source:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(
        UiUtils.createLink("Wikipedia - List of Java bytecode instructions", "https://en.wikipedia.org/wiki/List_of_Java_bytecode_instructions"),
        bag.next().fillCellHorizontally().weightx(1.0).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.LARGE_VGAP)
      )
    }, Dimension(800, 500), project, IdeModalityType.MODELESS)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class InstructionsTable : JBTable(InstructionsTableModel()), DataProvider {

    init {
      autoResizeMode = JTable.AUTO_RESIZE_OFF
      autoscrolls = true

      columnModel.apply {
        getColumn(0).minWidth = 100
        getColumn(1).minWidth = 30
        getColumn(2).minWidth = 120
        getColumn(3).minWidth = 180
        getColumn(4).preferredWidth = getMaxRowWith(4) + 2
      }

      TableSpeedSearch.installOn(this)

      rowSorter = TableRowSorter(model).apply {
        setComparator(1, OPCODE_COMPARATOR)
      }

      addMouseListener(UiUtils.Table.createContextMenuMouseListener(InstructionsOverviewTool::class.java.simpleName) {
        DefaultActionGroup().apply {
          add(CopyValueAction())
          add(ViewValueAction())
        }
      })
    }

    override fun getData(dataId: String): Any? = when {
      CommonDataKeys.VALUE.`is`(dataId) -> getSingleSelectedValue(this)
      else -> null
    }

    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
      val cellComponent: Component = super.prepareRenderer(renderer, row, column)
      if (cellComponent is JComponent) {
        cellComponent.toolTipText = when (column) {
          2 -> "[count]: [operand labels]"
          3 -> "[before] → [after]"
          else -> null
        }
      }
      return cellComponent
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class InstructionsTableModel : DefaultTableModel() {

    init {
      setDataVector(readInstructionsData(), COLUMNS)
    }

    override fun isCellEditable(row: Int, column: Int) = false

    private fun readInstructionsData(): Vector<Vector<String>> {
      val quotechar = '"'
      val separator = '|'
      val dataCsv = InstructionsOverviewTool::class.java.getResourceAsStream("/dev/turingcomplete/intellijbytecodeplugin/byte-code-instructions.csv")
        ?: throw IllegalStateException("snh: byte-code-instructions.csv missing")
      return Vector(InputStreamReader(dataCsv, "UTF-8").useLines {
        it.map { line ->
          //Part of the code comes from https://github.com/JetBrains/intellij-community/blob/master/platform/platform-util-io/src/com/intellij/execution/process/impl/CSVReader.java
          val tokensOnThisLine: MutableList<String> = ArrayList()
          var sb = StringBuilder()
          var inQuotes = false
          var i = 0
          while (i < line.length) {
            val c = line[i]
            if (c == quotechar) {
              // this gets complex... the quote may end a quoted block, or escape another quote.
              // do a 1-char lookahead:
              if ( // there is indeed another character to check.
                inQuotes && line.length > i + 1 && line[i + 1] == quotechar) { // ..and that char. is a quote also.
                // we have two quote chars in a row == one quote char, so consume them both and
                // put one on the token. we do *not* exit the quoted text.
                sb.append(line[i + 1])
                i++
              }
              else {
                inQuotes = !inQuotes
                // the tricky case of an embedded quote in the middle: a,bc"d"ef,g
                if ( //not at the begining of an escape sequence
                  (i > 2 && line[i - 1] != separator) && line.length > (i + 1) && line[i + 1] != separator //not at the     end of an escape sequence
                ) {
                  sb.append(c)
                }
              }
            }
            else if (c == separator && !inQuotes) {
              tokensOnThisLine.add(sb.toString())
              sb = StringBuilder() // start work on next token
            }
            else {
              sb.append(c)
            }
            i++

          }
          tokensOnThisLine.add(sb.toString())
          Vector(tokensOnThisLine)
        }.toList()
      })
    }
  }
}
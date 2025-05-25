package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.copyable
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideRightInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin._ui.toMonospace
import dev.turingcomplete.intellijbytecodeplugin._ui.withCommonsDefaults
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import org.jdesktop.swingx.HorizontalLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.*
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

internal class AccessConverterTool : ByteCodeTool("Access Converter", AllIcons.Nodes.RwAccess), DocumentListener, ActionListener {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private const val ACCESS_CALCULATORS_IN_ONE_ROW = 2

    private var ACCESS_REG_EX: Pattern = Pattern.compile("^\\d+$")
  }

  // -- Properties ---------------------------------------------------------- //

  private val accessType = ComboBox(EnumComboBoxModel(AccessGroup::class.java))
  private val accessField = IntegerField(null, 0, 99999)
  private val resultLabel = JBLabel().apply {
    font = JBFont.label().toMonospace()
  }.copyable()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun execute(project: Project?) {
    if (project?.isDisposed == true) {
      return
    }

    accessType.addActionListener(this)
    accessField.document.addDocumentListener(this)
    accessField.text = "1028"

    UiUtils.Dialog.show("Access Converter", ScrollPaneFactory.createScrollPane(JPanel(GridBagLayout()).apply {
      val bag = GridBag().withCommonsDefaults().setDefaultAnchor(GridBagConstraints.WEST)

      add(JLabel("Access:"), bag.nextLine().next())
      add(accessField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())
      add(accessType, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

      add(JLabel("Plain text:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(resultLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

      add(createAccessCalculatorsComponent(), bag.nextLine().next().coverLine().fillCell().weightx(1.0).weighty(1.0).overrideTopInset(UIUtil.LARGE_VGAP))
    }, true), Dimension(700, 600), project)
  }

  override fun actionPerformed(e: ActionEvent?) {
    calculateResult()
  }

  override fun insertUpdate(e: DocumentEvent?) {
    calculateResult()
  }

  override fun removeUpdate(e: DocumentEvent?) {
    calculateResult()
  }

  override fun changedUpdate(e: DocumentEvent?) {
    calculateResult()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun calculateResult() {
    val text = accessField.text
    resultLabel.text = if (ACCESS_REG_EX.asMatchPredicate().test(text)) {
      accessType.item.toReadableAccess(text.toInt()).joinToString(" ")
    }
    else {
      ""
    }
  }

  private fun createAccessCalculatorsComponent(): JComponent {
    return JPanel(GridBagLayout()).apply {
      border = IdeBorderFactory.createTitledBorder("Access Calculators")

      val accessGroups = AccessGroup.entries.toTypedArray()

      val bag = GridBag().withCommonsDefaults()
        .setDefaultFill(GridBagConstraints.BOTH)
        .setDefaultWeightX(0.25)
        .setDefaultWeightY(accessGroups.size.toDouble() / ACCESS_CALCULATORS_IN_ONE_ROW)

      var x = 0
      var y = 0
      for (i in accessGroups.indices) {
        val cellBag = (if (x == 0) bag.nextLine().next() else bag.next())
        if (y > 0) {
          cellBag.overrideTopInset(UIUtil.LARGE_VGAP)
        }
        cellBag.overrideRightInset(UIUtil.DEFAULT_HGAP * 4)
        add(AccessCalculatorPanel(accessGroups[i]), cellBag)

        if ((i + 1) % ACCESS_CALCULATORS_IN_ONE_ROW == 0) {
          x = 0
          y++
        }
        else {
          x++
        }
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  internal class AccessCalculatorPanel(accessGroup: AccessGroup) : BorderLayoutPanel(0, UIUtil.DEFAULT_HGAP / 2) {

    private val resultLabel = JBLabel("0").apply {
      font = JBFont.label().toMonospace()
    }.copyable()

    init {
      addToTop(JLabel(accessGroup.toString()))

      val model = AccessCalculatorTableModel(accessGroup) { resultLabel.text = it }
      val table = AccessCalculatorTable(model)
      addToCenter(ScrollPaneFactory.createScrollPane(table))

      addToBottom(JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP / 2)).apply {
        add(JBLabel("Access:"))
        add(resultLabel)
      })
    }
  }

  // -- Inner Type ---------------------------------------------------------- //


  private class AccessCalculatorTable(model: TableModel) : JBTable(model) {

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

  // -- Inner Type ---------------------------------------------------------- //

  private class AccessCalculatorTableModel(accessGroup: AccessGroup, private val setResult: (String) -> Unit) :
    DefaultTableModel(
      accessGroup.accesses.map { arrayOf(false, it.name.lowercase(Locale.getDefault()), it.value) }.toTypedArray(),
      arrayOf("", "Name", "Value")
    ) {

    init {
      updateResult()
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnIndex == 0

    override fun setValueAt(value: Any, rowIndex: Int, columnIndex: Int) {
      super.setValueAt(value, rowIndex, columnIndex)
      updateResult()
    }

    override fun getColumnClass(columnIndex: Int) = when (columnIndex) {
      0 -> java.lang.Boolean::class.java
      else -> String::class.java
    }

    private fun updateResult() {
      val resultValue = dataVector.filter { it[0] == true }.map { it[2] as Int }.reduceOrNull { a, b -> a.or(b) } ?: 0
      setResult(resultValue.toString())
    }
  }
}

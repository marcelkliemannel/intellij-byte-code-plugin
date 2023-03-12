package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.*
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

internal class AccessConverterTool : ByteCodeTool("Access Converter", AllIcons.Nodes.RwAccess), DocumentListener, ActionListener {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val ACCESS_CALCULATORS_IN_ONE_ROW = 2

    private var ACCESS_REG_EX: Pattern = Pattern.compile("^\\d+$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val accessType = ComboBox(EnumComboBoxModel(AccessGroup::class.java))
  private val accessField = UiUtils.Field.createNumberField()
  private val resultLabel = JBLabel().copyable()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun execute(project: Project?) {
    if (project?.isDisposed == true) {
      return
    }

    accessType.addActionListener(this)
    accessField.document.addDocumentListener(this)

    UiUtils.Dialog.show("Access Converter", ScrollPaneFactory.createScrollPane(JPanel(GridBagLayout()).apply {
      val bag = GridBag().withCommonsDefaults().setDefaultAnchor(GridBagConstraints.WEST)

      add(JLabel("Access:"), bag.nextLine().next())
      add(accessField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())
      add(accessType, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

      add(JLabel("Result:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(resultLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

      add(createAccessCalculatorsComponent(), bag.nextLine().next().coverLine().fillCell().weightx(1.0).weighty(1.0).overrideTopInset(UIUtil.LARGE_VGAP))
    }, true), Dimension(650, 500), project)
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun calculateResult() {
    val text = accessField.text
    resultLabel.text = if (ACCESS_REG_EX.asMatchPredicate().test(text)) {
      accessType.item.toReadableAccess(text.toInt()).joinToString(", ")
    }
    else {
      ""
    }
  }

  private fun createAccessCalculatorsComponent(): JComponent {
    return JPanel(GridBagLayout()).apply {
      border = IdeBorderFactory.createTitledBorder("Access Calculators")

      val accessGroups = AccessGroup.values()

      val bag = GridBag().withCommonsDefaults()
              .setDefaultFill(GridBagConstraints.BOTH)
              .setDefaultWeightX(0.25)
              .setDefaultWeightY(accessGroups.size.toDouble() / ACCESS_CALCULATORS_IN_ONE_ROW)

      var x = 0
      var y = 0
      for (i in 0 until accessGroups.size) {
        val cellBag = (if (x == 0) bag.nextLine().next() else bag.next())
        if (y > 0) {
          cellBag.overrideTopInset(UIUtil.LARGE_VGAP)
        }
        if (x > 0) {
          cellBag.overrideLeftInset(UIUtil.DEFAULT_HGAP)
        }
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

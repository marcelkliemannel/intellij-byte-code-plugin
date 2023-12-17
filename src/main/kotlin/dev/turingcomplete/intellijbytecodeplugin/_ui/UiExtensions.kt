package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.table.TableCellRenderer

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

fun JBFont.toMonospace(): JBFont = JBFont.create(Font(Font.MONOSPACED, this.style, this.size))

fun GridBag.withCommonsDefaults() = this
        .setDefaultAnchor(GridBagConstraints.NORTHWEST)
        .setDefaultInsets(0, 0, 0, 0)
        .setDefaultFill(GridBagConstraints.NONE)

fun JComponent.configureForCell(tree: JTree, selected: Boolean, hasFocus: Boolean): JComponent {
  val background = RenderingUtil.getBackground(tree, selected)
  val backgroundToUse: Color = when {
    selected -> background
    hasFocus -> RenderingUtil.getHoverBackground(tree) ?: background
    else -> background
  }
  return configureForCell(tree, RenderingUtil.getForeground(tree, selected), backgroundToUse)
}

fun JComponent.configureForCell(table: JTable, selected: Boolean, hasFocus: Boolean): JComponent {
  val background = RenderingUtil.getBackground(table, selected)
  val backgroundToUse: Color = when {
    selected -> background
    hasFocus -> RenderingUtil.getHoverBackground(table) ?: background
    else -> background
  }
  return configureForCell(table, RenderingUtil.getForeground(table, selected), backgroundToUse)
}

fun JComponent.configureForCell(cellContainer: JComponent, foreground: Color, background: Color): JComponent {
  this.foreground = foreground
  this.background = background
  componentOrientation = cellContainer.componentOrientation
  font = cellContainer.font
  isEnabled = cellContainer.isEnabled
  border = JBUI.Borders.empty(2, 3)
  return this
}

fun GridBag.overrideLeftInset(leftInset: Int): GridBag {
  this.insets(this.insets.top, leftInset, this.insets.bottom, this.insets.right)
  return this
}

fun GridBag.overrideRightInset(rightInset: Int): GridBag {
  this.insets(this.insets.top, this.insets.left, this.insets.bottom, rightInset)
  return this
}

fun GridBag.overrideBottomInset(bottomInset: Int): GridBag {
  this.insets(this.insets.top, this.insets.left, bottomInset, this.insets.right)
  return this
}

fun GridBag.overrideTopInset(topInset: Int): GridBag {
  this.insets(topInset, this.insets.left, this.insets.bottom, this.insets.right)
  return this
}

fun JBLabel.copyable(): JBLabel {
  setCopyable(true)
  return this
}

fun JTable.getMaxRowWith(column: Int): Int {
  var width = 0

  for (row in 0 until rowCount) {
    val renderer: TableCellRenderer = getCellRenderer(row, column)
    val comp = prepareRenderer(renderer, row, column)
    width = (comp.preferredSize.width + 1).coerceAtLeast(width)
  }

  return width
}

fun JComponent.widthStrictWidth(width: Int): JComponent {
  this.maximumSize = Dimension(width, this.maximumSize.height)
  this.preferredSize = Dimension(width, this.preferredSize.height)
  this.minimumSize = Dimension(width, this.minimumSize.height)
  return this
}

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //

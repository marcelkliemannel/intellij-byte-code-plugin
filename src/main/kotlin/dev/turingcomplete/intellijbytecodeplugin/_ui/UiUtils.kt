package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.render.RenderingUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.text.DecimalFormat
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.table.TableCellRenderer
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

internal object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun createDefaultGridBag() = GridBag()
          .setDefaultAnchor(GridBagConstraints.NORTHWEST)
          .setDefaultInsets(0, 0, 0, 0)
          .setDefaultFill(GridBagConstraints.NONE)

  fun createLink(title: String, url: String): HyperlinkLabel {
    return HyperlinkLabel(title).apply {
      setHyperlinkTarget(url)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Field {
    private var NUMBER_FIELD_REG_EX: Pattern = Pattern.compile("^\\d+$")

    fun createNumberField(value: Int? = null, columns: Int = 0) = JFormattedTextField(DecimalFormat("0")).apply {
      this.value = value
      this.columns = columns
      this.background = UIUtil.getTextFieldBackground()

      (this.document as AbstractDocument).documentFilter = object : DocumentFilter() {
        override fun replace(fb: FilterBypass?, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
          val matcher: Matcher = NUMBER_FIELD_REG_EX.matcher(text)
          if (!matcher.matches()) {
            return
          }
          super.replace(fb, offset, length, text, attrs)
        }

        override fun insertString(fb: FilterBypass?, offset: Int, string: String?, attr: AttributeSet?) {
          val matcher: Matcher = NUMBER_FIELD_REG_EX.matcher(text)
          if (!matcher.matches()) {
            return
          }
          super.insertString(fb, offset, string, attr)
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Dialog {
    fun show(title: String, content: JComponent, size: Dimension, project: Project?, ideModalityType: IdeModalityType = IdeModalityType.IDE) {
      object : DialogWrapper(project, true, ideModalityType) {
        init {
          this.title = title
          setSize(size.width, size.height)
          init()
        }

        override fun createActions() = arrayOf(myOKAction)

        override fun createCenterPanel() = content
      }.show()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object PopUp {
    fun showTextAreaPopup(value: String, dataContext: DataContext) {
      val valueTextArea = Panel.NotEditableTextArea(value, true)
      JBPopupFactory.getInstance()
              .createComponentPopupBuilder(valueTextArea, valueTextArea)
              .setRequestFocus(true)
              .setFocusable(true)
              .setResizable(true)
              .setMovable(true)
              .setModalContext(false)
              .setShowShadow(true)
              .setShowBorder(true)
              .setCancelKeyEnabled(true)
              .setCancelOnClickOutside(true)
              .setCancelOnOtherWindowOpen(true)
              .createPopup()
              .showInBestPositionFor(dataContext)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  object Panel {
    fun gridBag(init: (JPanel.() -> Unit)? = null): JPanel = JPanel(GridBagLayout()).apply { init?.invoke(this) }

    fun scroll(content: JComponent,
               vsbPolicy: Int = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
               hsbPolicy: Int = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER,
               withoutBorder: Boolean = true): JBScrollPane {
      return JBScrollPane(content, vsbPolicy, hsbPolicy).apply {
        if (withoutBorder) {
          border = JBUI.Borders.empty()
          viewportBorder = JBUI.Borders.empty()
        }
      }
    }

    class NotEditableTextArea(value: String, withoutBorder: Boolean = false) : BorderLayoutPanel() {

      init {
        val textArea = JBTextArea(value).apply {
          isEditable = false
        }

        addToCenter(ScrollPaneFactory.createScrollPane(textArea, withoutBorder))
      }
    }
  }
}

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
  border = JBUI.Borders.empty(2, 3, 2, 3)
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
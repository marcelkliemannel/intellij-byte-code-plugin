package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import java.text.DecimalFormat
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JFormattedTextField
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

internal object UiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

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

package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.rd.util.getThrowableText
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindow
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import java.awt.Component
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

abstract class ErrorStateHandler {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(ErrorStateHandler::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val inErrorState = AtomicBoolean()
  private val componentContainer = BorderLayoutPanel()
  private var centerComponent: JComponent? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createComponent(selected: Boolean = false): JComponent {
    if (selected) {
      initComponent()
    }

    return componentContainer
  }

  protected abstract fun createCenterComponent(): JComponent

  protected abstract fun retry()

  fun onError(message: String, cause: Throwable) {
    LOG.warn(message, cause)

    inErrorState.set(true)
    ApplicationManager.getApplication().invokeLater {
      componentContainer.apply {
        val retry = {
          inErrorState.set(false)
          removeAll()

          if (centerComponent == null) {
            initComponent()
          }
          else {
            addToCenter(centerComponent!!)
          }

          revalidate()
          repaint()
          retry()
        }
        removeAll()
        addToCenter(ErrorStatePanel(message, cause, retry))
        revalidate()
        repaint()
      }
    }
  }

  protected fun initComponent() {
    if (centerComponent == null) {
      centerComponent = createCenterComponent()
      componentContainer.addToCenter(centerComponent!!)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ErrorStatePanel(message: String, cause: Throwable, retry: () -> Unit) : JPanel(GridBagLayout()) {

    init {
      border = JBEmptyBorder(UIUtil.getRegularPanelInsets())

      val bag = UiUtils.createDefaultGridBag()

      val box = Box(BoxLayout.Y_AXIS)
      box.add(Box.createVerticalGlue())
      // Error
      val errorText = if (cause.message != null) "<b>$message:<br />${cause.message}</b>" else "<b>$message.</b>"
      box.add(JBLabel("<html>${errorText}</html>", AllIcons.General.BalloonError, SwingConstants.CENTER).apply {
        alignmentX = Component.CENTER_ALIGNMENT
      })

      box.add(Box.createVerticalStrut(UIUtil.DEFAULT_HGAP))

      // Retry
      box.add(JButton().apply {
        action = object : AbstractAction("Retry", AllIcons.Actions.Refresh) {
          override fun actionPerformed(e: ActionEvent?) {
            retry()
          }
        }
        alignmentX = Component.CENTER_ALIGNMENT
        requestFocusInWindow()
      })
      box.add(Box.createVerticalGlue())
      add(box, bag.nextLine().next().weightx(1.0).weighty(1.0).fillCell())

      // Stack trace
      add(JButton().apply {
        action = object : AbstractAction("Show full error stack trace...") {
          override fun actionPerformed(e: ActionEvent?) {
            val errorStackTraceTextArea = JBScrollPane(JTextArea(cause.getThrowableText()).apply { isEditable = false }).apply {
              putClientProperty(UIUtil.KEEP_BORDER_SIDES, SideBorder.ALL)
            }

            UiUtils.Dialog.show("Full Error Stack Trace", errorStackTraceTextArea, Dimension(600, 500), null)
          }
        }
      }, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))

      // Hint
      add(JBLabel("<html>Please create a bug for the ${ByteCodeToolWindow.PLUGIN_NAME} plugin if this error should not occur.</html>",
                  AllIcons.General.BalloonInformation, SwingConstants.LEFT),
          bag.nextLine().next().weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))
    }
  }
}
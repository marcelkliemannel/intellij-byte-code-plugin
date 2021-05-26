package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.rd.util.getThrowableText
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

abstract class ErrorStateHandler {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
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

      val errorText = if (cause.message != null) "<b>$message: ${cause.message}</b>" else "<b>$message.</b>"


      add(JPanel(), bag.nextLine().next().weighty(0.05).fillCell())


      add(JBLabel("<html>${errorText}</html>", AllIcons.General.BalloonError, SwingConstants.CENTER),
          bag.nextLine().next().weightx(1.0).anchor(GridBagConstraints.CENTER).fillCell())

      add(JButton().apply {
        action = object : AbstractAction("Retry", AllIcons.Actions.Refresh) {
          override fun actionPerformed(e: ActionEvent?) {
            retry()
          }
        }
      }, bag.nextLine().next().anchor(GridBagConstraints.CENTER).overrideTopInset(UIUtil.LARGE_VGAP))


      add(JPanel(), bag.nextLine().next().weighty(0.05).fillCell())


      val errorStackTrace = JBScrollPane(JTextArea(cause.getThrowableText()).apply { isEditable = false }).apply {
        putClientProperty(UIUtil.KEEP_BORDER_SIDES, SideBorder.ALL)
      }
      add(UI.PanelFactory.panel(errorStackTrace)
                  .resizeY(true).resizeX(true)
                  .withLabel("Full error stack strace:").anchorLabelOn(UI.Anchor.Top).moveLabelOnTop()
                  .createPanel(),
          bag.nextLine().next().weightx(1.0).weighty(0.9).fillCell())
    }
  }
}
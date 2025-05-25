package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodePluginIcons
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin._ui.withCommonsDefaults
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils
import dev.turingcomplete.intellijbytecodeplugin.common._internal.DataProviderUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.analysis.AnalyzerException
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.CheckClassAdapter
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.PrintWriter
import java.io.StringWriter
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.SwingConstants

@Suppress("ComponentNotRegistered")
internal class VerifyByteCodeAction : ByteCodeAction("Verify Byte Code", null, ByteCodePluginIcons.VERIFY_ICON) {

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    val classFileContext = DataProviderUtils.getData(CommonDataKeys.CLASS_FILE_CONTEXT_DATA_KEY, e.dataContext)

    val onError = DataProviderUtils.getData(CommonDataKeys.ON_ERROR_DATA_KEY, e.dataContext)

    val verifyByteCode = {
      val resultWriter = StringWriter()
      val printWriter = PrintWriter(resultWriter)
      CheckClassAdapter.verify(classFileContext.classReader(), true, printWriter)

      val output = resultWriter.toString()
      val success = !output.contains(AnalyzerException::class.java.name)
      ClassFileContext.VerificationResult(success, output)
    }
    AsyncUtils.runAsync(classFileContext.project(), verifyByteCode, { result ->
      ApplicationManager.getApplication().invokeLater {
        val resultPanel = VerifyByteCodeResultPanel(result)
        UiUtils.Dialog.show("Verify Byte Code Result", resultPanel, Dimension(800, 450), classFileContext.project())
      }
    }, { cause -> onError("Failed execute byte code verification", cause) })
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class VerifyByteCodeResultPanel(result: ClassFileContext.VerificationResult) : JPanel(GridBagLayout()) {
    init {
      val bag = GridBag().withCommonsDefaults().setDefaultFill(GridBagConstraints.HORIZONTAL)

      val stateLabel = if (result.success) {
        JBLabel("Byte code verified.", AllIcons.General.InspectionsOK, SwingConstants.LEFT)
      }
      else {
        JBLabel("Byte code verification failed. See output for failure details.", AllIcons.General.BalloonError, SwingConstants.LEFT)
      }
      add(stateLabel.apply { font = font.deriveFont(Font.BOLD) }, bag.nextLine().next())

      add(JBLabel("Output:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP))
      val resultTextArea = JBTextArea(result.output, 20, 100).apply {
        val globalScheme = EditorColorsManager.getInstance().globalScheme
        font = JBUI.Fonts.create(globalScheme.editorFontName, globalScheme.editorFontSize)
      }
      add(ScrollPaneFactory.createScrollPane(resultTextArea).apply {
        border = BorderFactory.createLineBorder(JBColor.border())
      }, bag.nextLine().next().fillCell().weightx(1.0).weighty(1.0).overrideTopInset(2))
    }
  }
}

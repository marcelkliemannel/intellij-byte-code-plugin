package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.copyable
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideTopInset
import dev.turingcomplete.intellijbytecodeplugin.bytecode.Access
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.signature.SignatureReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.TraceSignatureVisitor
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import org.apache.commons.lang.StringEscapeUtils
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SignatureParserTool : ByteCodeTool("Signature Parser") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val SAMPLES = arrayOf(
            "Select sample",
            "(TR;)I",
            "<T::Ljava/util/function/Function;E:Ljava/lang/IllegalArgumentException;>([Ljava/lang/String;TT;)V^TE;"
    )

    private val FORMAT_TYPE_PARAMETER_REGEX = Regex("^((?<formalTypeParameter><.*>)\\()(?<declaration>.*)$")

    fun parseSignature(signature: String): ParsingResult {
      try {
        val traceSignatureVisitor = TraceSignatureVisitor(Access.INTERFACE.value)
        SignatureReader(signature).accept(traceSignatureVisitor)

        val returnType = traceSignatureVisitor.returnType

        var formalTypeParameter: String? = null
        val declaration: String
        val declarationSplit = FORMAT_TYPE_PARAMETER_REGEX.matchEntire(traceSignatureVisitor.declaration)
        if (declarationSplit != null) {
          formalTypeParameter = declarationSplit.groups["formalTypeParameter"]!!.value
          declaration = "(${declarationSplit.groups["declaration"]!!.value}"
        }
        else {
          declaration = traceSignatureVisitor.declaration
        }

        val exceptions = traceSignatureVisitor.exceptions

        return ParsingResult(null, Signature(returnType, formalTypeParameter, declaration, exceptions))
      }
      catch (e: Exception) {
        return ParsingResult(e, null)
      }
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun execute(project: Project?) {
    SignatureParserDialog(project).show()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class SignatureParserDialog(project: Project?) : DialogWrapper(project), DocumentListener {

    private val signatureField = JBTextField(30)
    private val isInterface = JBCheckBox("Is from interface")

    private val resultContainer = JPanel(GridBagLayout())

    init {
      this.title = "Signature Parser"
      setSize(500, 150)
      init()

      signatureField.document.addDocumentListener(this)
    }

    override fun createCenterPanel() = JPanel(GridBagLayout()).apply {
      val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

      add(JLabel("Signature:"), bag.nextLine().next())
      add(signatureField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())
      add(isInterface, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

      add(resultContainer, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP).coverLine().weightx(1.0).fillCellHorizontally())

      val samplesComboBox = ComboBox(SAMPLES).apply {
        addItemListener {
          if (selectedIndex > 0) {
            signatureField.text = SAMPLES[selectedIndex]
          }
        }
      }

      add(samplesComboBox, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP).coverLine())

      add(JPanel(), bag.nextLine().next().fillCell().coverLine().weighty(1.0).weightx(1.0))
    }

    override fun insertUpdate(e: DocumentEvent?) {
      parseSignature()
    }

    override fun removeUpdate(e: DocumentEvent?) {
      parseSignature()
    }

    override fun changedUpdate(e: DocumentEvent?) {
      parseSignature()
    }

    override fun createActions() = arrayOf(myOKAction)

    private fun parseSignature() {
      resultContainer.removeAll()

      val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)

      val parsingResult = parseSignature(signatureField.text)
      if (parsingResult.error != null) {
        val errorMessage = "Invalid signature${if (parsingResult.error.message != null) " ${parsingResult.error.message}" else ""}"
        resultContainer.add(JBLabel(errorMessage, AllIcons.General.BalloonError, SwingConstants.LEFT), bag.nextLine().next().weightx(1.0).fillCellHorizontally())
      }
      else {
        val signature = parsingResult.signature

        val rows = mutableListOf<Pair<String, String>>()
        if (signature.returnType != null) {
          rows.add(Pair("Return type:", signature.returnType))
        }
        if (signature.formalTypeParameter != null) {
          rows.add(Pair("Type parameter:", signature.formalTypeParameter))
        }
        rows.add(Pair("Declaration:", signature.declaration))
        if (signature.exceptions != null) {
          rows.add(Pair("Exceptions:", signature.exceptions))
        }

        rows.forEachIndexed { index, row ->
          val topInset = if (index == 0) 0 else UIUtil.DEFAULT_VGAP
          resultContainer.add(JBLabel(row.first), bag.nextLine().next().overrideTopInset(topInset))
          resultContainer.add(JBLabel("<html>${StringEscapeUtils.escapeHtml(row.second)}</html>").copyable(), bag.next().overrideTopInset(topInset).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())
        }
      }

      pack()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ParsingResult internal constructor(val error: Exception?, private val _signature: Signature?) {

    val signature: Signature
      get() {
        if (error != null) {
          throw IllegalStateException("Parsing resulted in an error state.")
        }
        return _signature!!
      }
  }

  class Signature internal constructor(val returnType: String?,
                                       val formalTypeParameter: String?,
                                       val declaration: String,
                                       val exceptions: String?)
}
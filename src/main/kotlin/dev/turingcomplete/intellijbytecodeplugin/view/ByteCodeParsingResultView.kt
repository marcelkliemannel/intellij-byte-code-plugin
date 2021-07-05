package dev.turingcomplete.intellijbytecodeplugin.view

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsActions
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.ui.DumbAwareActionButton
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.DocumentUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.FilesDropHandler
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.common.OpenInEditorAction
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

abstract class ByteCodeParsingResultView(classFileContext: ClassFileContext,
                                         title: String,
                                         icon: Icon,
                                         private val goToMethodsRegex: Regex? = null)
  : ByteCodeView(classFileContext, title, icon) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val editor: EditorEx by lazy { createEditor() }
  private val parsingIndicatorLabel by lazy { JBLabel("Parsing...") }

  private val parsingResultCache: MutableMap<Int, ByteCodeParsingResult> = mutableMapOf()

  private val goToMethods = mutableListOf<Pair<Int, String>>()
  private val goToMethodsLink: DropDownLink<Pair<Int, String>> by lazy { createGoToMethodsLink() }

  private var skipDebug = false
  private var skipCode = false
  private var skipFrame = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // Sync editor colors, if IntelliJ appearance changed
    ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(LafManagerListener.TOPIC, LafManagerListener { editor.syncEditorColors() })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun doSelected() {
    editor.component.requestFocusInWindow()
  }

  override fun createCenterComponent(): JComponent {
    return SimpleToolWindowPanel(true, false).apply {
      toolbar = JPanel(GridBagLayout()).apply {
        val bag = UiUtils.createDefaultGridBag().setDefaultAnchor(GridBagConstraints.WEST)
        add(createToolbarActionsComponent(), bag.nextLine().next().fillCellHorizontally().weightx(1.0))
        add(parsingIndicatorLabel.apply { border = JBUI.Borders.empty(2) }, bag.next().fillCellVertically())
        add(goToMethodsLink, bag.next().fillCellHorizontally().overrideLeftInset(2).overrideLeftInset(2))
      }

      setContent(editor.component)

      asyncParseByteCode()
    }
  }

  protected abstract fun parseByteCode(parsingOptions: Int): String

  protected open fun openInEditorFileName(): String = "${classFileContext.classFile().nameWithoutExtension}.txt"

  protected open fun additionalToolBarActions(): ActionGroup? = null

  override fun dispose() {
    if (!editor.isDisposed) {
      EditorFactory.getInstance().releaseEditor(editor)
    }

    super.dispose()
  }

  override fun retry() {
    asyncParseByteCode()
  }

  protected fun getText(): String? = if (isByteCodeParsingResultAvailable()) editor.document.text else null

  protected fun setText(text: String) {
    val gotToMethods = parseGoToMethods(text).toList().sortedBy { it.second }
    goToMethods.clear()
    goToMethods.addAll(gotToMethods)

    DocumentUtil.writeInRunUndoTransparentAction {
      editor.document.apply {
        setReadOnly(false)
        setText(text)
        setReadOnly(true)
      }
      editor.component.requestFocusInWindow()
    }
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.`is`(dataId) && isByteCodeParsingResultAvailable() -> {
        return LightVirtualFile(openInEditorFileName(), editor.document.text)
      }
      else -> super.getData(dataId)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createGoToMethodsLink(): DropDownLink<Pair<Int, String>> {
    val createPopUp: (DropDownLink<Pair<Int, String>>) -> JBPopup = {
      JBPopupFactory.getInstance()
              .createPopupChooserBuilder(goToMethods)
              .setRenderer(GoToMethodsCellRenderer())
              .setItemChosenCallback { goToMethod(it.first) }
              .createPopup()
    }
    return object : DropDownLink<Pair<Int, String>>(Pair(-1, "Go to method"), createPopUp) {
      override fun itemToString(item: Pair<Int, String>) = item.second
    }
  }

  private fun goToMethod(line: Int) {
    if (line > 0) {
      val logicalPosition = LogicalPosition(line, 0)
      this@ByteCodeParsingResultView.editor.scrollingModel.scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE)
      this@ByteCodeParsingResultView.editor.caretModel.addCaret(logicalPosition, true)
    }
  }

  private fun createToolbarActionsComponent(): JComponent {
    val toolbarActionsGroup = DefaultActionGroup().apply {
      add(object : DefaultActionGroup("Parsing Options", true) {
        init {
          templatePresentation.icon = AllIcons.General.Filter

          add(ToggleParsingOptionAction("Skip Debug Information", { skipDebug }, { skipDebug = !skipDebug }))
          add(ToggleParsingOptionAction("Skip Method Code", { skipCode }, { skipCode = !skipCode }))
          add(ToggleParsingOptionAction("Skip Frames", { skipFrame }, { skipFrame = !skipFrame }))
        }

        override fun update(e: AnActionEvent) {
          e.presentation.isEnabled = isByteCodeParsingResultAvailable()
        }
      })

      addSeparator()

      add(OpenInEditorAction())

      additionalToolBarActions()?.let { addAll(it) }

      addAllByteCodeActions()
    }

    return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActionsGroup, true).component
  }

  private fun createEditor(): EditorEx {
    val document = EditorFactory.getInstance().createDocument("")
    return EditorFactory.getInstance().createViewer(document, classFileContext.project()).let { it as EditorEx }.apply {
      val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(JavaFileType.INSTANCE, project, null)
      highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().globalScheme)

      (this as EditorImpl).setDropHandler(FilesDropHandler(classFileContext.project()))

      syncEditorColors()

      setBorder(JBUI.Borders.empty())
      setCaretVisible(true)

      settings.apply {
        settings.isLineMarkerAreaShown = true
        settings.isIndentGuidesShown = true
        settings.isLineNumbersShown = true
        settings.isFoldingOutlineShown = true
      }
    }
  }

  private fun asyncParseByteCode() {
    parsingIndicatorLabel.isVisible = true

    AsyncUtils.runAsync(classFileContext.project(), doParseByteCode(), { result ->
      goToMethods.clear()
      goToMethods.addAll(result.goToMethods.toList().sortedBy { it.second })

      ApplicationManager.getApplication().invokeLater {
        // Text
        DocumentUtil.writeInRunUndoTransparentAction {
          editor.document.apply {
            setReadOnly(false)
            setText(result.text)
            setReadOnly(true)
          }
          editor.component.requestFocusInWindow()
        }

        // Go to
        goToMethodsLink.isEnabled = goToMethods.isNotEmpty()

        parsingIndicatorLabel.isVisible = false
      }
    }, { cause -> onError("Failed to parse byte code", cause) })
  }

  private fun isByteCodeParsingResultAvailable() = !parsingIndicatorLabel.isVisible

  private fun doParseByteCode(): () -> ByteCodeParsingResult = {
    parsingResultCache.computeIfAbsent(calculateParsingOptions()) { parsingOptions ->
      val text = parseByteCode(parsingOptions)
      val lineOfMethod = parseGoToMethods(text)
      ByteCodeParsingResult(text, lineOfMethod)
    }
  }

  private fun parseGoToMethods(text: String): Map<Int, String> {
    val lineOfMethod = mutableMapOf<Int, String>()
    if (goToMethodsRegex != null) {
      text.lines().forEachIndexed { lineNumber, line ->
        val methodMatcher = goToMethodsRegex.matchEntire(line)
        if (methodMatcher != null) {
          lineOfMethod[lineNumber] = methodMatcher.groups["name"]!!.value
        }
      }
    }
    return lineOfMethod
  }

  private fun calculateParsingOptions(): Int {
    var parsingOptions = 0
    if (skipCode) {
      parsingOptions += ClassReader.SKIP_CODE
    }
    if (skipDebug) {
      parsingOptions += ClassReader.SKIP_DEBUG
    }
    if (skipFrame) {
      parsingOptions += ClassReader.SKIP_FRAMES
    }
    return parsingOptions
  }

  private fun EditorEx.syncEditorColors() {
    setBackgroundColor(null) // To use background from set color scheme

    val isLaFDark = ColorUtil.isDark(UIUtil.getPanelBackground())
    val isEditorDark = EditorColorsManager.getInstance().isDarkEditor
    colorsScheme = if (isLaFDark == isEditorDark) {
      EditorColorsManager.getInstance().globalScheme
    }
    else {
      EditorColorsManager.getInstance().schemeForCurrentUITheme
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ByteCodeParsingResult(val text: String, val goToMethods: Map<Int, String>)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class ToggleParsingOptionAction(@NlsActions.ActionText text: String,
                                                private val isSelected: () -> Boolean,
                                                private val toggleSelected: () -> Unit) : DumbAwareActionButton(text) {

    override fun actionPerformed(e: AnActionEvent) {
      toggleSelected()
      asyncParseByteCode()
    }

    override fun updateButton(e: AnActionEvent) {
      e.presentation.icon = if (isSelected()) PlatformIcons.CHECK_ICON else EmptyIcon.create(PlatformIcons.CHECK_ICON)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class GoToMethodsCellRenderer : DefaultListCellRenderer() {

    @Suppress("UNCHECKED_CAST")
    override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focused: Boolean): Component {
      super.getListCellRendererComponent(list, value, index, selected, false)

      value as Pair<Int, String>
      text = "${value.second} (line: ${value.first + 1})"

      border = JBUI.Borders.empty(0, 5, 0, 10)
      if (!selected) {
        background = UIUtil.getLabelBackground()
      }

      return this
    }
  }
}
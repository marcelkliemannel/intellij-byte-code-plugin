package dev.turingcomplete.intellijbytecodeplugin.view

import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import com.intellij.util.asSafely
import com.intellij.util.ui.EmptyIcon
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory.Companion.TOOLBAR_PLACE_PREFIX
import dev.turingcomplete.intellijbytecodeplugin._ui.SimpleListCellRenderer
import dev.turingcomplete.intellijbytecodeplugin._ui.overrideLeftInset
import dev.turingcomplete.intellijbytecodeplugin._ui.withCommonsDefaults
import dev.turingcomplete.intellijbytecodeplugin.common.ByteCodeAnalyserSettingsService
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils.runAsync
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.FilesDropHandler
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeAction.Companion.addAllByteCodeActions
import dev.turingcomplete.intellijbytecodeplugin.view.common.OpenInEditorAction
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.Delegates

abstract class ByteCodeParsingResultView(
  classFileContext: ClassFileContext,
  title: String,
  private val goToMethodsRegex: Regex? = null,
  private val parsingOptionsAvailable: Boolean = true
) : ByteCodeView(classFileContext, title) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val editor: EditorEx by lazy { createEditor() }
  private var editorCreated: Boolean = false
  private val parsingIndicatorLabel = JBLabel("Parsing...")

  private val parsingResultCache: MutableMap<Int, ByteCodeParsingResult> = mutableMapOf()

  private val goToMethods = mutableListOf<Pair<Int, String>>()
  private val goToMethodsLink: DropDownLink<Pair<Int, String>> by lazy { createGoToMethodsLink() }

  private var skipDebug by Delegates.observable(ByteCodeAnalyserSettingsService.skipDebug) { _, _, new ->
    ByteCodeAnalyserSettingsService.skipDebug = new
  }
  private var skipCode by Delegates.observable(ByteCodeAnalyserSettingsService.skipCode) { _, _, new ->
    ByteCodeAnalyserSettingsService.skipCode = new
  }
  private var skipFrame by Delegates.observable(ByteCodeAnalyserSettingsService.skipFrame) { _, _, new ->
    ByteCodeAnalyserSettingsService.skipFrame = new
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    // Sync editor colors after IntelliJ appearance changed
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
        val bag = GridBag().withCommonsDefaults().setDefaultAnchor(GridBagConstraints.WEST)
        add(createToolbarActionsComponent(this), bag.nextLine().next().fillCellHorizontally().weightx(1.0))
        add(parsingIndicatorLabel.apply { border = JBUI.Borders.empty(2) }, bag.next().fillCellVertically())
        add(goToMethodsLink, bag.next().fillCellHorizontally().overrideLeftInset(2).overrideLeftInset(2))
      }

      setContent(editor.component)

      asyncParseByteCode()
    }
  }

  protected open fun openInEditorFileName(): String = "${classFileContext.classFile().nameWithoutExtension}.txt"

  protected open fun additionalToolBarActions(): ActionGroup? = null

  override fun dispose() {
    if (editorCreated && !editor.isDisposed) {
      EditorFactory.getInstance().releaseEditor(editor)
    }

    super.dispose()
  }

  override fun reParseClassNodeContext() {
    asyncParseByteCode()
  }

  protected abstract fun asyncParseByteCode(parsingOptions: Int, onSuccess: (String) -> Unit)

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
        .setRenderer(SimpleListCellRenderer { it.asSafely<Pair<Int, String>>()?.let { "${it.second} (line: ${it.first + 1})" } ?: "" })
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

  private fun createToolbarActionsComponent(targetComponent: JComponent): JComponent {
    val toolbarActionsGroup = DefaultActionGroup().apply {
      addAllByteCodeActions()

      addSeparator()

      if (parsingOptionsAvailable) {
        add(object : DefaultActionGroup("Parsing Options", true) {
          init {
            templatePresentation.icon = AllIcons.General.Filter

            add(ToggleParsingOptionAction("Skip Debug Information", { skipDebug }, { skipDebug = !skipDebug }))
            add(ToggleParsingOptionAction("Skip Method Code", { skipCode }, { skipCode = !skipCode }))
            add(ToggleParsingOptionAction("Skip Frames", { skipFrame }, { skipFrame = !skipFrame }))
          }

          override fun update(e: AnActionEvent) {
            val enabled = isByteCodeParsingResultAvailable()
            e.presentation.isEnabled = enabled
          }

          override fun getActionUpdateThread() = ActionUpdateThread.BGT
        })
      }

      addSeparator()

      add(OpenInEditorAction())

      additionalToolBarActions()?.let { addAll(it) }
    }

    return ActionManager.getInstance().createActionToolbar("${TOOLBAR_PLACE_PREFIX}.parsingResultView", toolbarActionsGroup, true).run {
      setTargetComponent(targetComponent)
      component
    }
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
        isLineMarkerAreaShown = true
        isIndentGuidesShown = true
        isLineNumbersShown = true
        isFoldingOutlineShown = true
      }

      editorCreated = true
    }
  }

  private fun asyncParseByteCode() {
    ApplicationManager.getApplication().assertIsDispatchThread()

    val parsingOptions = calculateParsingOptions()
    if (parsingResultCache.containsKey(parsingOptions)) {
      parsingResultCache[parsingOptions]?.let {
        setByteCodeParsingResult(it.text, it.goToMethods)
      }
    }
    else {
      parsingIndicatorLabel.isVisible = true

      val setParsingResult: (String) -> Unit = { newText ->
        val newGoToMethods = parseGoToMethods(newText)
        parsingResultCache.computeIfAbsent(parsingOptions) { ByteCodeParsingResult(newText, newGoToMethods) }
        setByteCodeParsingResult(newText, newGoToMethods)
        ApplicationManager.getApplication().invokeLater { parsingIndicatorLabel.isVisible = false }
      }
      runAsync(classFileContext.project(),
               { asyncParseByteCode(parsingOptions, setParsingResult) },
               { cause -> onError("Failed to parse byte code", cause) })
    }
  }

  private fun setByteCodeParsingResult(newText: String, newGoToMethods: Map<Int, String>) {
    goToMethods.clear()
    goToMethods.addAll(newGoToMethods.toList().sortedBy { it.second })

    ApplicationManager.getApplication().invokeLater {
      goToMethodsLink.isEnabled = newGoToMethods.isNotEmpty()

      DocumentUtil.writeInRunUndoTransparentAction {
        editor.document.apply {
          setReadOnly(false)
          setText(newText)
          setReadOnly(true)
        }
        editor.component.requestFocusInWindow()
      }
    }
  }

  private fun isByteCodeParsingResultAvailable() = !parsingIndicatorLabel.isVisible

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

    override fun getActionUpdateThread() = ActionUpdateThread.EDT
  }
}
package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.JBColor
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.EmptyIcon
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesToolWindowAction
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.AnalyzeByteCodeAction
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.FilesDropHandler
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.dnd.DropTarget
import javax.swing.Icon

internal class ByteCodeToolWindowFactory : ToolWindowFactory, DumbAware {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOGGER = Logger.getInstance(ByteCodeToolWindowFactory::class.java)

    const val TOOL_WINDOW_ID = "Byte Code"
    const val PLUGIN_NAME = "Byte Code Analyzer"
    const val TOOLBAR_PLACE_PREFIX = "dev.turingcomplete.intellijbytecodeplugin.toolbar"

    fun <T> getData(dataProvider: DataProvider, dataKey: DataKey<T>): Any? {
      val project = PROJECT.getData(dataProvider) ?: return null
      val byteCodeToolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null

      val classFileTab = byteCodeToolWindow.contentManager.selectedContent?.getUserData(ClassFileTab.CLASS_FILE_TAB_KEY)
      return if (dataKey.`is`(ClassFileTab.CLASS_FILE_TAB_KEY.toString())) {
        classFileTab
      }
      else {
        return classFileTab?.getData(dataKey.name)
      }
    }

    fun openClassFile(classFile: ClassFile, toolWindow: ToolWindow, project: Project) {
      ApplicationManager.getApplication().invokeLater {
        val newClassFileTab = ClassFileTab(project, classFile)
        Disposer.register(toolWindow.disposable, newClassFileTab)
        val contentManager = toolWindow.contentManager
        val content = contentManager.factory.createContent(
          newClassFileTab.createComponent(true),
          classFile.file.nameWithoutExtension,
          true
        )
        content.putUserData(ClassFileTab.CLASS_FILE_TAB_KEY, newClassFileTab)
        contentManager.addContent(content)
        toolWindow.show {
          contentManager.setSelectedContent(content, true)
        }
      }
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    assert(toolWindow.id == TOOL_WINDOW_ID)

    toolWindow.apply {
      initDropTarget(project)
      setupEmptyText(project)
      initActions(project)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun ToolWindow.initDropTarget(project: Project) {
    ApplicationManager.getApplication().invokeLater {
      if (project.isDisposed) {
        return@invokeLater
      }

      try {
        @Suppress("USELESS_ELVIS") // NPE happened in production
        val component = contentManager.component ?: return@invokeLater

        val toolWindowDropTarget = component.dropTarget
        if (toolWindowDropTarget != null) {
          toolWindowDropTarget.addDropTargetListener(FilesDropHandler(project))
        }
        else {
          contentManager.component.dropTarget = DropTarget(contentManager.component, FilesDropHandler(project))
        }
      } catch (e: Exception) {
        // This method sometimes throws exceptions, because the UI is in an
        // invalid state. For example:
        // - `ContentManager#getComponent()` -> `NullPointerException` -> "because "this.myUI" is null"
        // - `DropTarget#addDropTargetListener()` -> `TooManyListenersException`
        LOGGER.warnWithDebug("snh: Failed to set drop target listener.", e)
      }
    }
  }

  private fun ToolWindow.setupEmptyText(project: Project) {
    if (this !is ToolWindowEx) {
      // Should not happen because an unchecked cast to ToolWindowEx is done
      // all other the core IntelliJ code.
      return
    }

    ApplicationManager.getApplication().invokeLater {
      emptyText?.apply {
        clear()

        isCenterAlignText = false

        appendLine("To open class files, do one of the following:", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)

        // The following code is used to create some indent to highlight the
        // following options from the previous line.
        val leftIndent = 7
        val fakeIndentIcon = EmptyIcon.create(leftIndent, 16)
        val fakeIndentIconAsIconBackground = EmptyIcon.create(16 + leftIndent, 16)
        val indentIcon = { icon: Icon? ->
          if (icon != null) {
            val wrapperIcon = LayeredIcon(2)
            wrapperIcon.setIcon(fakeIndentIconAsIconBackground, 0)
            wrapperIcon.setIcon(icon, 1, leftIndent, 0)
            wrapperIcon
          }
          else {
            fakeIndentIcon
          }
        }

        appendLine(indentIcon(null), "From the context menu action '${AnalyzeByteCodeAction.TITLE}'", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)

        OpenClassFilesToolWindowAction.EP.extensions.forEach { openClassFilesAction ->
          appendLine(indentIcon(openClassFilesAction.icon), openClassFilesAction.linkTitle, SimpleTextAttributes.LINK_ATTRIBUTES) {
            openClassFilesAction.execute(project)
          }
        }

        appendLine(indentIcon(AllIcons.Actions.Download), "Drop class files here to open", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)

        component.background = JBColor.background()
      }
    }
  }

  private fun ToolWindow.initActions(project: Project) {
    ApplicationManager.getApplication().invokeLater {
      val byteCodeToolsActionGroup = DefaultActionGroup("Byte Code Tools", true).apply {
        templatePresentation.icon = AllIcons.General.ExternalTools
        ByteCodeTool.EP.extensions.forEach { add(it.toAction()) }

        addSeparator()

        addAll(ByteCodeRelatedLinksActionsGroup(), ReportAnIssueAction())
      }
      setTitleActions(listOf(byteCodeToolsActionGroup))

      if (this is ToolWindowEx) {
        val newSessionActionsGroup = DefaultActionGroup(OpenClassFilesOptionsAction(project, contentManager))
        setTabActions(newSessionActionsGroup)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenClassFilesOptionsAction(project: Project, private val contentManager: ContentManager)
    : DefaultActionGroup("Open Class Files", null, AllIcons.General.Add) {

    init {
      OpenClassFilesToolWindowAction.EP.extensions.forEach { add(it.createAsEmbeddedAction(project)) }
      isPopup = true
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = contentManager.contents.isNotEmpty()
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ReportAnIssueAction : DumbAwareAction("Report an Issue") {

    override fun actionPerformed(e: AnActionEvent) {
      AsyncUtils.browseAsync(e.project, "https://github.com/marcelkliemannel/intellij-byte-code-plugin/issues")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ByteCodeRelatedLinksActionsGroup : DefaultActionGroup("Byte Code Related Links", true) {

    val links = mapOf(
      Pair("Oracle: Java Language and Virtual Machine Specifications", "https://docs.oracle.com/javase/specs/index.html"),
      Pair("Wikipedia: Java class file", "https://en.wikipedia.org/wiki/Java_class_file"),
      Pair("Wikipedia: Java bytecode instruction listings", "https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings"),
      Pair("ASM: Java bytecode manipulation and analysis framework", "https://asm.ow2.io")
    )

    init {
      links.forEach { (title, link) -> add(createLinkAction(title, link)) }
    }

    private fun createLinkAction(title: String, link: String): AnAction = object : DumbAwareAction(title) {

      override fun actionPerformed(e: AnActionEvent) {
        AsyncUtils.browseAsync(e.project, link)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.JBColor
import com.intellij.ui.LayeredIcon
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentManager
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.EmptyIcon
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesListener
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesToolWindowAction
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.AnalyzeByteCodeAction
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.FilesDropHandler
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.OpenClassFilesTask
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.dnd.DropTarget
import javax.swing.Icon

internal class ByteCodeToolWindow : ToolWindowFactory, DumbAware, Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val ID = "Byte Code"
    const val PLUGIN_NAME = "Byte Code Analyzer"

    fun <T> getData(dataProvider: DataProvider, dataKey: DataKey<T>): Any? {
      val project = dataProvider.getData(PROJECT.name).castSafelyTo<Project>() ?: return null
      val byteCodeToolWindow = ToolWindowManager.getInstance(project).getToolWindow(ID) ?: return null
      return byteCodeToolWindow.contentManager.selectedContent
              ?.getUserData(ClassFileTab.CLASS_FILE_TAB_KEY)
              ?.selectedByteCodeView
              ?.getData(dataKey.name)
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    assert(toolWindow.id == ID)

    Disposer.register(toolWindow.disposable, this)

    toolWindow.apply {
      initDropTarget(project)
      setupEmptyText(project)
      initActions(project)
    }

    project.messageBus.connect(this).apply {
      subscribe(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC, MyOpenClassFilesListener(project, toolWindow.contentManager))
    }
  }

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun ToolWindow.initDropTarget(project: Project) {
    val toolWindowDropTarget = contentManager.component.dropTarget
    if (toolWindowDropTarget != null) {
      toolWindowDropTarget.addDropTargetListener(FilesDropHandler(project))
    }
    else {
      contentManager.component.dropTarget = DropTarget(contentManager.component, FilesDropHandler(project))
    }
  }

  private fun ToolWindow.setupEmptyText(project: Project) {
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

      appendLine(indentIcon(null), "From the action '${AnalyzeByteCodeAction.TITLE}'", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)

      OpenClassFilesToolWindowAction.EP.extensions.forEach { openClassFilesAction ->
        appendLine(indentIcon(openClassFilesAction.icon), openClassFilesAction.linkTitle, SimpleTextAttributes.LINK_ATTRIBUTES) {
          openClassFilesAction.execute(project)
        }
      }

      appendLine(indentIcon(AllIcons.Actions.Download), "Drop class files here to open", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)

      component.background = JBColor.background()
    }
  }

  private fun ToolWindow.initActions(project: Project) {
    setTitleActions(listOf(ByteCodeToolsActionsGroup()))

    if (this is ToolWindowEx) {
      val newSessionActionsGroup = DefaultActionGroup(OpenClassFilesOptionsAction(project, contentManager))
      setTabActions(newSessionActionsGroup)

      val additionalGearActionsGroup = DefaultActionGroup(HelpLinksActionsGroup())
      setAdditionalGearActions(additionalGearActionsGroup)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenClassFilesOptionsAction(project: Project, private val contentManager: ContentManager)
    : DefaultActionGroup("Open Class Files", true) {

    init {
      OpenClassFilesToolWindowAction.EP.extensions.forEach { add(it.createAsEmbeddedAction(project)) }
      templatePresentation.icon = AllIcons.General.Add
      templatePresentation.isVisible = contentManager.contents.isNotEmpty()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isVisible = contentManager.contents.isNotEmpty()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ByteCodeToolsActionsGroup : DefaultActionGroup("Byte Code Tools", true) {

    init {
      ByteCodeTool.EP.extensions.forEach { add(it.toAction()) }
      templatePresentation.icon = AllIcons.General.ExternalTools
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class HelpLinksActionsGroup : DefaultActionGroup("Help Links", true) {

    val links = mapOf(Pair("Oracle: Java Language and Virtual Machine Specifications", "https://docs.oracle.com/javase/specs/index.html"),
                      Pair("Wikipedia: Java class file", "https://en.wikipedia.org/wiki/Java_class_file"),
                      Pair("Wikipedia: Java bytecode instruction listings", "https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings"),
                      Pair("ASM: Java bytecode manipulation and analysis framework", "https://asm.ow2.io"))

    init {
      links.forEach { (title, link) -> add(createLinkAction(title, link)) }
      templatePresentation.icon = AllIcons.Actions.Help
    }

    private fun createLinkAction(title: String, link: String): AnAction = object : DumbAwareAction(title) {

      override fun actionPerformed(e: AnActionEvent) {
        BrowserUtil.browse(link)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class MyOpenClassFilesListener(private val project: Project, private val contentManager: ContentManager) : OpenClassFilesListener {

    private val openClassFile: (VirtualFile) -> Unit = { openClassFile(it) }

    override fun openPsiFiles(psiFiles: List<PsiFile>) {
      OpenClassFilesTask(openClassFile, project).consumePsiFiles(psiFiles).openFiles()
    }

    override fun openPsiElements(psiElements: List<PsiElement>, originPsiFile: PsiFile?) {
      OpenClassFilesTask(openClassFile, project).consumePsiElements(psiElements, originPsiFile).openFiles()
    }

    override fun openFiles(files: List<VirtualFile>) {
      OpenClassFilesTask(openClassFile, project).consumeFiles(files).openFiles()
    }

    private fun openClassFile(classFile: VirtualFile) {
      ApplicationManager.getApplication().invokeLater {
        assert(classFile.extension == "class")

        val newClassFileTab = ClassFileTab(project, classFile)
        Disposer.register(this@ByteCodeToolWindow, newClassFileTab)
        val content = contentManager.factory.createContent(newClassFileTab.createComponent(true),
                                                           classFile.nameWithoutExtension,
                                                           true)
        content.putUserData(ClassFileTab.CLASS_FILE_TAB_KEY, newClassFileTab)
        contentManager.addContent(content)
        contentManager.setSelectedContent(content, true)
      }
    }
  }
}
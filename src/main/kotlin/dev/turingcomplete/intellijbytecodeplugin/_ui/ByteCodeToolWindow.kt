package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
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
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.content.ContentManager
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.StatusText
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesListener
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesToolWindowAction
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.FilesDropHandler
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.OpenClassFilesTask
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.dnd.DropTarget


class ByteCodeToolWindow : ToolWindowFactory, DumbAware, Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val ID = "Byte Code"

    fun <T> getData(dataProvider: DataProvider, dataKey: DataKey<T>) : Any? {
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

    val toolWindowDropTarget = toolWindow.contentManager.component.dropTarget
    if (toolWindowDropTarget != null) {
      toolWindowDropTarget.addDropTargetListener(FilesDropHandler(project))
    }
    else {
      toolWindow.contentManager.component.dropTarget = DropTarget(toolWindow.contentManager.component, FilesDropHandler(project))
    }

    toolWindow.emptyText?.initEmptyText(project)

    initActions(toolWindow, project)

    project.messageBus.connect(this).apply {
      subscribe(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC, MyOpenClassFilesListener(project, toolWindow.contentManager))
    }
  }

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun StatusText.initEmptyText(project: Project) {
    OpenClassFilesToolWindowAction.EP.extensions.forEach { openClassFilesAction ->
      appendLine(openClassFilesAction.icon, openClassFilesAction.title, SimpleTextAttributes.LINK_ATTRIBUTES) {
        openClassFilesAction.execute(project)
      }
      appendLine("")
    }

    appendLine(AllIcons.Actions.Download, "Drop .class files here to open", SimpleTextAttributes.REGULAR_ATTRIBUTES, null)
  }

  private fun initActions(toolWindow: ToolWindow, project: Project) {
    toolWindow.setTitleActions(listOf(ByteCodeToolsActionsGroup()))

    if (toolWindow is ToolWindowEx) {
      val newSessionActionsGroup = DefaultActionGroup(OpenClassFilesOptionsAction(project, toolWindow.contentManager))
      toolWindow.setTabActions(newSessionActionsGroup)

      val additionalGearActionsGroup = DefaultActionGroup(HelpLinksActionsGroup())
      toolWindow.setAdditionalGearActions(additionalGearActionsGroup)
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

    override fun openPsiElement(psiElement: PsiElement) {
      OpenClassFilesTask(openClassFile, project).consumePsiElement(psiElement).openFiles()
    }

    override fun openFiles(files: List<VirtualFile>) {
      OpenClassFilesTask(openClassFile, project).consumeFiles(files).openFiles()
    }

    private fun openClassFile(classFile: VirtualFile) {
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
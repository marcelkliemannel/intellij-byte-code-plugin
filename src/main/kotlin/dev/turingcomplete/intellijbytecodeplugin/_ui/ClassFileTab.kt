package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.TabbedPaneWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile.CompilableSourceFile
import dev.turingcomplete.intellijbytecodeplugin.common._internal.AsyncUtils
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.ClassFilePreparationTask
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import dev.turingcomplete.intellijbytecodeplugin.view._internal.ErrorStateHandler
import javax.swing.JComponent
import javax.swing.SwingConstants

internal class ClassFileTab(
  private val project: Project,
  private var classFile: ClassFile
) : ErrorStateHandler(), DataProvider, DumbAware, Disposable {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val CLASS_FILE_TAB_KEY = Key<ClassFileTab>("dev.turingcomplete.intellijbytecodeplugin.classFileTab")
  }

  // -- Properties ---------------------------------------------------------- //

  private var byteCodeViewTabs: ByteCodeViewTabs? = null
  private val centerComponentContainer = BorderLayoutPanel()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun dispose() {
  }

  override fun createCenterComponent(): JComponent {
    loadClassNodeContext()
    return centerComponentContainer
  }

  override fun reParseClassNodeContext() {
    val sourceFile = classFile.sourceFile
    if (sourceFile is CompilableSourceFile) {
      val classFileCandidates = ClassFileCandidates.fromAbsolutePaths(classFile.file.toNioPath())
      val classFilePreparationTask = ClassFilePreparationTask(classFileCandidates, sourceFile)
      project.getService(ClassFilesPreparatorService::class.java)
        .prepareClassFiles(listOf(classFilePreparationTask), centerComponentContainer) {
          classFile = it
          doReParseClassNodeContext()
        }
    }
    else {
      doReParseClassNodeContext()
    }
  }

  private fun doReParseClassNodeContext() {
    ApplicationManager.getApplication().invokeLater {
      val previousSelectedByteCodeViewIndex = byteCodeViewTabs?.selectedByteCodeViewIndex
      loadClassNodeContext(previousSelectedByteCodeViewIndex)
    }
  }

  override fun getData(dataId: String): Any? {
    if (byteCodeViewTabs == null) {
      return null
    }

    // We are inside the EDT -> no threading issues
    return byteCodeViewTabs!!.selectedBytecodeView().getData(dataId)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun loadClassNodeContext(selectedByteCodeViewIndex: Int? = 0) {
    setCenter(JBLabel("Parsing '${classFile.file.nameWithoutExtension}'...", AnimatedIcon.Default(), SwingConstants.CENTER))

    val createClassFileContext = {
      // If we reach this method through the "refresh class file" action, it
      // may happen that the file has changed on the disc, but the `VirtualFile`
      // does not pick up this change and the `VirtualFile#inputStream` returns
      // an outdated cached version.
      if (!classFile.refreshValidity()) {
        throw IllegalStateException("Class file no longer exists.")
      }
      DefaultClassFileContext(project, classFile, true)
    }
    val onSuccess: (ClassFileContext) -> Unit = { classFileContext ->
      ApplicationManager.getApplication().invokeLater {
        loadTabs(classFileContext, selectedByteCodeViewIndex ?: 0)
      }
    }
    val onError: (Throwable) -> Unit = { cause ->
      onError("Failed to parse class file '${classFile.file.name}'", cause)
    }
    AsyncUtils.runAsync(project, createClassFileContext, onSuccess, onError)
  }

  private fun loadTabs(classFileContext: ClassFileContext, selectedByteCodeViewIndex: Int) {
    if (project.isDisposed) {
      return
    }

    byteCodeViewTabs = ByteCodeViewTabs(classFileContext, this)
    // We are inside the EDT -> no threading issues
    setCenter(byteCodeViewTabs!!.component)

    if (selectedByteCodeViewIndex != 0) {
      // We are inside the EDT -> no threading issues
      byteCodeViewTabs!!.selectBytecodeViewIndex(selectedByteCodeViewIndex)
    }
  }

  private fun setCenter(component: JComponent) {
    centerComponentContainer.apply {
      removeAll()
      addToCenter(component)
      revalidate()
      repaint()
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ByteCodeViewTabs(classFileContext: ClassFileContext, parentDisposable: Disposable)
    : TabbedPaneWrapper(parentDisposable) {

    private val byteCodeViews: List<ByteCodeView> = ByteCodeView.EP.extensions.mapIndexed { index, byteCodeViewCreator ->
      val selected = index == 0
      val classFileView = byteCodeViewCreator.create(classFileContext)
      addTab(classFileView.title, null, classFileView.createComponent(selected), null)
      Disposer.register(parentDisposable, classFileView)
      classFileView
    }
    var selectedByteCodeViewIndex: Int = 0

    init {
      addChangeListener {
        if (selectedByteCodeViewIndex != selectedIndex) {
          selectedByteCodeViewIndex = selectedIndex
          byteCodeViews[selectedByteCodeViewIndex].selected()
        }
      }
    }

    fun selectedBytecodeView() = byteCodeViews[selectedByteCodeViewIndex]

    fun selectBytecodeViewIndex(index: Int) {
      setSelectedIndex(index, true)
    }
  }
}

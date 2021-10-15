package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.TabbedPaneWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeView
import dev.turingcomplete.intellijbytecodeplugin.view._internal.ErrorStateHandler
import javax.swing.JComponent
import javax.swing.SwingConstants

internal class ClassFileTab(private val project: Project, private val classFile: VirtualFile)
  : ErrorStateHandler(), DataProvider, DumbAware, Disposable {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val CLASS_FILE_TAB_KEY = Key<ClassFileTab>("dev.turingcomplete.intellijbytecodeplugin.classFileTab")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var byteCodeViewTabs: ByteCodeViewTabs? = null
  private val centerComponentContainer = BorderLayoutPanel()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun dispose() {
  }

  override fun createCenterComponent(): JComponent {
    loadClassNodeContext()
    return centerComponentContainer
  }

  override fun reParseClassNodeContext() {
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun loadClassNodeContext(selectedByteCodeViewIndex: Int? = 0) {
    setCenter(JBLabel("Parsing '${classFile.nameWithoutExtension}'...", AnimatedIcon.Default(), SwingConstants.CENTER))

    val onSuccess = loadTabs(selectedByteCodeViewIndex ?: 0)
    DefaultClassFileContext.loadAsync(project, classFile, onSuccess) { cause ->
      onError("Failed to parse class file '${classFile.name}'", cause)
    }
  }

  private fun loadTabs(selectedByteCodeViewIndex: Int): (ClassFileContext) -> Unit = { classFileContext ->
    ApplicationManager.getApplication().invokeLater {
      if (project.isDisposed) {
        return@invokeLater
      }

      byteCodeViewTabs = ByteCodeViewTabs(classFileContext, this)
      // We are inside the EDT -> no threading issues
      setCenter(byteCodeViewTabs!!.component)

      if (selectedByteCodeViewIndex != 0) {
        // We are inside the EDT -> no threading issues
        byteCodeViewTabs!!.selectBytecodeViewIndex(selectedByteCodeViewIndex)
      }
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ByteCodeViewTabs(classFileContext: ClassFileContext, parentDisposable: Disposable)
    : TabbedPaneWrapper(parentDisposable) {

    private val byteCodeViews: List<ByteCodeView>
    var selectedByteCodeViewIndex: Int

    init {
      byteCodeViews = ByteCodeView.EP.extensions.mapIndexed { index, byteCodeViewCreator ->
        val selected = index == 0
        val classFileView = byteCodeViewCreator.create(classFileContext)
        addTab(classFileView.title, classFileView.icon, classFileView.createComponent(selected), null)
        Disposer.register(parentDisposable, classFileView)
        classFileView
      }
      selectedByteCodeViewIndex = 0

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
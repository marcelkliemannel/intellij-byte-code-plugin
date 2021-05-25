package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
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

class ClassFileTab(private val project: Project, private val classFile: VirtualFile)
  : ErrorStateHandler(), DumbAware, Disposable {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var byteCodeViews : List<ByteCodeView>
  private val centerComponentContainer = BorderLayoutPanel()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun dispose() {
  }

  override fun createCenterComponent(): JComponent {
    loadClassNodeContext()
    return centerComponentContainer
  }

  override fun retry() {
    loadClassNodeContext()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun loadClassNodeContext() {
    setCenter(JBLabel("Parsing '${classFile.nameWithoutExtension}'...", AnimatedIcon.Default(), SwingConstants.CENTER))

    ApplicationManager.getApplication().executeOnPooledThread {
      DefaultClassFileContext.loadAsync(project, classFile, loadTabs()) { cause ->
        onError("Failed to parse class file '${classFile.name}'", cause)
      }
    }
  }

  private fun loadTabs(): (ClassFileContext) -> Unit = { classFileContext ->
    ApplicationManager.getApplication().invokeLater {
      if (project.isDisposed) {
        return@invokeLater
      }

      val tabs = TabbedPaneWrapper(this).apply {
        byteCodeViews = ByteCodeView.EP.extensions.mapIndexed { index, byteCodeViewCreator ->
          val classFileView = byteCodeViewCreator.create(classFileContext)
          addTab(classFileView.title, classFileView.icon, classFileView.createComponent(index == 0), null)
          Disposer.register(this@ClassFileTab, classFileView)
          classFileView
        }
      }
      tabs.addChangeListener { byteCodeViews[tabs.selectedIndex].selected() }
      setCenter(tabs.component)
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
}
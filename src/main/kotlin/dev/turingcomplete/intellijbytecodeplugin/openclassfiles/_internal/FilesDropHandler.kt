package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.ide.dnd.FileCopyPasteUtil
import com.intellij.ide.dnd.TransferableWrapper
import com.intellij.openapi.editor.EditorDropHandler
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import dev.turingcomplete.intellijbytecodeplugin._ui.NotificationUtils
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles.OpenClassFilesListener
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import javax.swing.JComponent
import javax.swing.TransferHandler

internal class FilesDropHandler(private val project: Project) : TransferHandler(), EditorDropHandler, DropTargetListener {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun canImport(comp: JComponent?, transferFlavors: Array<out DataFlavor>?): Boolean {
    return canHandleDrop0(transferFlavors)
  }

  override fun canHandleDrop(transferFlavors: Array<out DataFlavor>?): Boolean {
    return canHandleDrop0(transferFlavors)
  }

  override fun drop(event: DropTargetDropEvent) {
    event.acceptDrop(event.dropAction)
    handleDrop0(event.transferable)
  }

  override fun handleDrop(transferable: Transferable, project: Project?, editorWindow: EditorWindow?) {
    handleDrop0( transferable)
  }

  override fun importData(comp: JComponent?, transferable: Transferable): Boolean {
    return handleDrop0(transferable)
  }

  override fun dragEnter(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dragOver(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dropActionChanged(dtde: DropTargetDragEvent?) {
    // Nothing to do
  }

  override fun dragExit(dte: DropTargetEvent?) {
    // Nothing to do
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun canHandleDrop0(transferFlavors: Array<out DataFlavor>?): Boolean {
    return transferFlavors != null && FileCopyPasteUtil.isFileListFlavorAvailable(transferFlavors)
  }

  private fun handleDrop0(transferable: Transferable): Boolean {
    try {
      // Handle drop of PSI elements
      if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        val transferData = transferable.getTransferData(DataFlavor.javaFileListFlavor)
        if (transferData is TransferableWrapper) {
          project.messageBus.syncPublisher(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC).openPsiElements(transferData.psiElements?.toList() ?: listOf())
          return true
        }
      }

      // Handle drop of other elements
      val virtualFileManager = VirtualFileManager.getInstance()
      FileCopyPasteUtil.getFileList(transferable)?.mapNotNull { virtualFileManager.findFileByNioPath(it.toPath()) }?.let {
        project.messageBus.syncPublisher(OpenClassFilesListener.OPEN_CLASS_FILES_TOPIC).openFiles(it)
        return true
      }
    }
    catch (e: Exception) {
      NotificationUtils.notifyInternalError("Open .class files", "Failed to handle dropped files: ${e.message}.", e, project)
    }

    return false
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
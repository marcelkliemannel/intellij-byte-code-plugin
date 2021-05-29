package dev.turingcomplete.intellijbytecodeplugin.openclassfiles

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.messages.Topic

interface OpenClassFilesListener {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @Topic.ProjectLevel
    val OPEN_CLASS_FILES_TOPIC = Topic(OpenClassFilesListener::class.java, Topic.BroadcastDirection.NONE)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun openPsiFiles(psiFiles: List<PsiFile>)

  fun openPsiElements(psiElements: List<PsiElement>)

  fun openFiles(files: List<VirtualFile>)

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
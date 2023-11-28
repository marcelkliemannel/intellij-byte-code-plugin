package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView

internal class DecompiledView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "Decompiled", parsingOptionsAvailable = false) {

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun asyncParseByteCode(parsingOptions: Int, onSuccess: (String) -> Unit) {
    val classFile = classFileContext.classFile()
    val outermostClassFileName = "${classFile.name.substringBefore('$')}.class"
    val isNestedClassFile = classFile.name != outermostClassFileName

    // The `IdeaDecompiler` from the bundled Jetbrains Java Decompiler
    // plugin, will require to accept a legal text before its usage. For
    // that, a dialog needs to be opened through the
    // `Before#beforeFileOpened` listener. If the legal text does not
    // get accepted (or the dialog does not get opened), IntelliJ will
    // do a fallback to its default decompiler, which may not be able to
    // decompile method bodies of Java class files.
    classFileContext.project().messageBus.syncPublisher(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER)
      .beforeFileOpened(FileEditorManager.getInstance(classFileContext.project()), classFile)

    val decompilers = ClassFileDecompilers.getInstance().EP_NAME.extensions
      .filter { it.accepts(classFile) }
      // Prefer the `IdeaDecompiler` decompiler, as it produces better results.
      .sortedBy { it.javaClass.name != IDEA_DECOMPILER_FQ_CLASS_NAME }
      .toList()

    val psiManager = PsiManager.getInstance(classFileContext.project())
    var decompiledSourceCode: String? = decompileClassFile(classFile, decompilers, psiManager)
    if (decompiledSourceCode == null && isNestedClassFile) {
      // Some decompilers have trouble decompiling standalone nested class
      // files. What might help is to decompile the outermost class. Some
      // decompilers will then include the nested class in that file.
      val outermostClassFile = classFile.parent.findFile(outermostClassFileName)
      if (outermostClassFile != null) {
        decompiledSourceCode = decompileClassFile(outermostClassFile, decompilers, psiManager)
      }
    }
    if (decompiledSourceCode != null) {
      onSuccess(decompiledSourceCode)
    }
    else {
      var errorMessage = "The class file could not be decompiled by any of the available decompilers."
      val decompilerPlugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.java.decompiler"))
      if (decompilerPlugin == null || !decompilerPlugin.isEnabled) {
        errorMessage += " Try to install or enable JetBrain's 'Java Bytecode Decompiler' plugin."
      }
      onError(errorMessage)
    }
  }

  private fun decompileClassFile(
    classFile: VirtualFile,
    decompilers: List<ClassFileDecompilers.Decompiler>,
    psiManager: PsiManager
  ): String? {
    decompilers.forEach { decompiler ->
      if (decompiler is ClassFileDecompilers.Full) {
        val createFileViewProvider = decompiler.createFileViewProvider(classFile, psiManager, true)
        val decompiledText = createFileViewProvider.getPsi(createFileViewProvider.baseLanguage)?.text
        if (!decompiledText.isNullOrBlank()) {
          return decompiledText
        }
      }
      else if (decompiler is ClassFileDecompilers.Light) {
        val decompiledText = decompiler.getText(classFile).toString()
        if (decompiledText.isNotBlank()) {
          return decompiledText
        }
      }
    }
    return null
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.`is`(dataId) -> classFileContext.classFile()

      else -> super.getData(dataId)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = DecompiledView(classFileContext)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val IDEA_DECOMPILER_FQ_CLASS_NAME = "org.jetbrains.java.decompiler.IdeaDecompiler"
  }
}
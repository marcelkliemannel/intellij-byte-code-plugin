package dev.turingcomplete.intellijbytecodeplugin.view._internal._decompiler

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView

internal class DecompiledView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "Decompiled", parsingOptionsAvailable = false) {

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun asyncParseByteCode(parsingOptions: Int, onSuccess: (String) -> Unit) {
    val classFile = classFileContext.classFile()

    // The `IdeaDecompiler` from the bundled Jetbrains Java Decompiler
    // plugin, will require to accept a legal text before its usage. For
    // that, a dialog needs to be opened through the
    // `Before#beforeFileOpened` listener. If the legal text does not
    // get accepted (or the dialog does not get opened), IntelliJ will
    // do a fallback to its default decompiler, which may not be able to
    // decompile method bodies of Java class files.
    ApplicationManager.getApplication().invokeAndWait {
      classFileContext.project().messageBus.syncPublisher(FileEditorManagerListener.Before.FILE_EDITOR_MANAGER)
        .beforeFileOpened(FileEditorManager.getInstance(classFileContext.project()), classFile.file)
    }

    val decompiledSourceCode = DecompilerUtils.decompile(classFile.file, classFileContext.project())
    if (decompiledSourceCode != null) {
      onSuccess(decompiledSourceCode)
    }
    else {
      var errorMessage = "The class file could not be decompiled by any of the available decompilers."
      val decompilerPlugin = PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.java.decompiler"))
      if (decompilerPlugin == null || !decompilerPlugin.isEnabled) {
        errorMessage += " Try to install or enable JetBrain's 'Java Bytecode Decompiler' plugin."
      }
      onError(errorMessage, IllegalArgumentException())
    }
  }

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.OPEN_IN_EDITOR_DATA_KEY.`is`(dataId) -> classFileContext.classFile().file

      else -> super.getData(dataId)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = DecompiledView(classFileContext)
  }

  // -- Companion Object ---------------------------------------------------- //
}

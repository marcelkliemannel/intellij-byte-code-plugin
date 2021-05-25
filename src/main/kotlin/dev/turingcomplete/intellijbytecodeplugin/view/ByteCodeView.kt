package dev.turingcomplete.intellijbytecodeplugin.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view._internal.ErrorStateHandler
import javax.swing.Icon

abstract class ByteCodeView(val classFileContext: ClassFileContext, val title: String, val icon: Icon)
  : ErrorStateHandler(), Disposable {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<Creator> = ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.byteCodeViewCreator")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun doGetData(dataId: String): Any? = when {
    CommonDataKeys.CLASS_FILE_CONTEXT_DATA_KEY.`is`(dataId) -> classFileContext
    else -> null
  }

  override fun dispose() {
    // Override if needed
  }

  fun selected() {
    initComponent()
    doSelected()
  }

  open fun doSelected() {
    // Override if needed
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  fun interface Creator {
    fun create(classFileContext: ClassFileContext): ByteCodeView
  }
}
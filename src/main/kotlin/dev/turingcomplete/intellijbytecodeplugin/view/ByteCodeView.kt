package dev.turingcomplete.intellijbytecodeplugin.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.extensions.ExtensionPointName
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view._internal.ErrorStateHandler

abstract class ByteCodeView(val classFileContext: ClassFileContext, val title: String) :
  ErrorStateHandler(), Disposable, DataProvider {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<Creator> =
      ExtensionPointName.create("dev.turingcomplete.intellijbytecodeplugin.byteCodeView")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

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

  override fun getData(dataId: String): Any? =
    when {
      CommonDataKeys.CLASS_FILE_CONTEXT_DATA_KEY.`is`(dataId) -> classFileContext
      CommonDataKeys.ON_ERROR_DATA_KEY.`is`(dataId) -> { message: String, cause: Throwable ->
          onError(message, cause)
        }
      else -> null
    }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  fun interface Creator {
    fun create(classFileContext: ClassFileContext): ByteCodeView
  }
}

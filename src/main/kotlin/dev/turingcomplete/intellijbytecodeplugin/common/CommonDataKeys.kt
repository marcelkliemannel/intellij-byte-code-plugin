package dev.turingcomplete.intellijbytecodeplugin.common

import com.intellij.openapi.actionSystem.DataKey

object CommonDataKeys {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val CLASS_FILE_CONTEXT_DATA_KEY = DataKey.create<ClassFileContext>("dev.turingcomplete.intellijbytecodeplugin.classFileContext")
  val ON_ERROR_DATA_KEY = DataKey.create<(String, Throwable) -> Unit>("dev.turingcomplete.intellijbytecodeplugin.onError")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.ide.actions.GotoClassAction
import com.intellij.ide.actions.SearchEverywhereBaseAction
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils

abstract class SearchProvider(val value: String, val searchAction: () -> SearchEverywhereBaseAction) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Class(internalName: String)
    : SearchProvider(TypeUtils.toReadableName(internalName, TypeUtils.TypeNameRenderMode.QUALIFIED), { GotoClassAction() })
}
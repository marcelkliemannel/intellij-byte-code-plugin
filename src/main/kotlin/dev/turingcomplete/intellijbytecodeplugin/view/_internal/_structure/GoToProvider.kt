package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.ide.actions.GotoClassAction
import com.intellij.ide.actions.SearchEverywhereBaseAction
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils

internal abstract class GoToProvider(
  val value: String,
  val goToAction: () -> SearchEverywhereBaseAction,
) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Class(internalName: String) :
    GoToProvider(
      TypeUtils.toReadableName(internalName, TypeUtils.TypeNameRenderMode.QUALIFIED),
      { GotoClassAction() },
    )
}

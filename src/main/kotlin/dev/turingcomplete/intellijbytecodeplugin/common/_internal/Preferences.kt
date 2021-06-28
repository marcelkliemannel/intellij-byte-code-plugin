package dev.turingcomplete.intellijbytecodeplugin.common._internal

import com.intellij.openapi.components.PersistentStateComponent

class PreferencesState : PersistentStateComponent<PreferencesState.State> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  
  override fun getState(): State? {
    TODO("Not yet implemented")
  }

  override fun loadState(state: State) {
    TODO("Not yet implemented")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class State {
    var value: String? = null
  }
}
package dev.turingcomplete.intellijbytecodeplugin.common._internal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.util.concurrent.Callable

internal object AsyncUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <V> runAsync(project: Project, execute: Callable<V>, onSuccess: (V) -> Unit, onError: (Throwable) -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        if (project.isDisposed) {
          return@executeOnPooledThread
        }

        onSuccess(execute.call())
      }
      catch (e: Throwable) {
        onError(e)
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
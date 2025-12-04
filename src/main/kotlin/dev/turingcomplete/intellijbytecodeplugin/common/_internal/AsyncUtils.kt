package dev.turingcomplete.intellijbytecodeplugin.common._internal

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory.Companion.PLUGIN_NAME
import java.nio.file.Path
import java.util.concurrent.Callable

internal object AsyncUtils {
  // -- Properties ---------------------------------------------------------- //

  private val log = Logger.getInstance(AsyncUtils::class.java)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun runAsync(project: Project?, execute: Runnable, onError: (Throwable) -> Unit) =
    runAsync(
      project,
      {
        execute.run()
        null
      },
      {},
      onError,
    )

  fun <V> runAsync(
    project: Project?,
    execute: Callable<V>,
    onSuccess: (V) -> Unit,
    onError: (Throwable) -> Unit,
  ) {
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        if (project?.isDisposed == true) {
          return@executeOnPooledThread
        }

        onSuccess(execute.call())
      } catch (e: Throwable) {
        onError(e)
      }
    }
  }

  fun browseAsync(project: Project?, url: String) {
    browseAsync(project, { BrowserUtil.browse(url) }, url)
  }

  fun browseAsync(project: Project?, path: Path) {
    browseAsync(project, { BrowserUtil.browse(path) }, path.toString())
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun browseAsync(project: Project?, runnable: Runnable, target: String) {
    runAsync(project, runnable) { e ->
      log.warn("Could not open: $target", e)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(
          project,
          "Could not open '$target': ${e.message ?: "Unknown error"}",
          PLUGIN_NAME,
        )
      }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
}

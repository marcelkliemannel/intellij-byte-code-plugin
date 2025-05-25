package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

internal object NotificationUtils {
  // -- Properties ---------------------------------------------------------- //

  private val logger = Logger.getInstance(NotificationUtils::class.java)
  private const val notificationGroupId =
    "dev.turingcomplete.intellijbytecodeplugin.notificationGroup"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun notifyInternalError(
    title: String,
    message: String,
    e: Throwable? = null,
    project: Project? = null,
  ) {
    logger.warn(message, e)

    ApplicationManager.getApplication().invokeLater {
      NotificationGroupManager.getInstance()
        .getNotificationGroup(notificationGroupId)
        .createNotification(
          title,
          "$message\nSee idea.log for more details.",
          NotificationType.ERROR,
        )
        .notify(project)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

package dev.turingcomplete.intellijbytecodeplugin._ui

import com.intellij.openapi.util.IconLoader

internal object ByteCodePluginIcons {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @JvmField
  val TOOL_WINDOW_ICON = IconLoader.getIcon("/icons/toolwindow.svg", ByteCodePluginIcons::class.java)

  val ACTION_ICON = IconLoader.getIcon("/icons/action.svg", ByteCodePluginIcons::class.java)

  val VERIFY_ICON = IconLoader.getIcon("/icons/verify.svg", ByteCodePluginIcons::class.java)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
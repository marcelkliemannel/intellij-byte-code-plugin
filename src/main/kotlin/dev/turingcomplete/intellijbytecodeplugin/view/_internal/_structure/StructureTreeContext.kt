package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils.MethodDescriptorRenderMode
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils.TypeNameRenderMode
import dev.turingcomplete.intellijbytecodeplugin.settings.ByteCodeAnalyserSettingsService
import kotlin.properties.Delegates

internal class StructureTreeContext(val project: Project, private val syncStructure: () -> Unit) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  var typeNameRenderMode: TypeNameRenderMode by Delegates.observable(ByteCodeAnalyserSettingsService.instance.typeNameRenderMode) { _, old, new ->
    ByteCodeAnalyserSettingsService.instance.typeNameRenderMode = new
    syncStructure(new, old)
  }
  var methodDescriptorRenderMode: MethodDescriptorRenderMode by Delegates.observable(ByteCodeAnalyserSettingsService.instance.methodDescriptorRenderMode) { _, old, new ->
    ByteCodeAnalyserSettingsService.instance.methodDescriptorRenderMode = new
    syncStructure(new, old)
  }
  var showAccessAsHex: Boolean by Delegates.observable(ByteCodeAnalyserSettingsService.instance.showAccessAsHex) { _, old, new ->
    ByteCodeAnalyserSettingsService.instance.showAccessAsHex = new
    syncStructure(new, old)
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //

  private fun <T> syncStructure(new: T, old: T) {
    if (new != old) {
      syncStructure.invoke()
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
}

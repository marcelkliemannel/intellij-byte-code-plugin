package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils.MethodDescriptorRenderMode
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils.TypeNameRenderMode
import dev.turingcomplete.intellijbytecodeplugin.common.ByteCodeAnalyserSettingsService
import kotlin.properties.Delegates

internal class StructureTreeContext(val project: Project, private val syncStructure: () -> Unit) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var typeNameRenderMode: TypeNameRenderMode by Delegates.observable(ByteCodeAnalyserSettingsService.typeNameRenderMode) { _, old, new ->
    ByteCodeAnalyserSettingsService.typeNameRenderMode = new
    syncStructure(new, old)
  }
  var methodDescriptorRenderMode: MethodDescriptorRenderMode by Delegates.observable(ByteCodeAnalyserSettingsService.methodDescriptorRenderMode) { _, old, new ->
    ByteCodeAnalyserSettingsService.methodDescriptorRenderMode = new
    syncStructure(new, old)
  }
  var showAccessAsHex: Boolean by Delegates.observable(ByteCodeAnalyserSettingsService.showAccessAsHex) { _, old, new ->
    ByteCodeAnalyserSettingsService.showAccessAsHex = new
    syncStructure(new, old)
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T> syncStructure(new: T, old: T) {
    if (new != old) {
      syncStructure.invoke()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
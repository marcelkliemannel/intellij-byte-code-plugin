package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StructureTreeContext(val project: Project, private val syncStructure: () -> Unit) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var typeNameRenderMode : TypeUtils.TypeNameRenderMode by Delegates.observable(TypeUtils.TypeNameRenderMode.QUALIFIED, syncStructure())
  var methodDescriptorRenderMode : MethodDeclarationUtils.MethodDescriptorRenderMode by Delegates.observable(MethodDeclarationUtils.MethodDescriptorRenderMode.DECLARATION, syncStructure())
  var showAccessAsHex : Boolean by Delegates.observable(true, syncStructure())

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T> syncStructure(): (property: KProperty<*>, oldValue: T, newValue: T) -> Unit = { _, new, old ->
    if (new != old) {
      syncStructure.invoke()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmMethodUtils
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTypeUtils
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class StructureTreeContext(val project: Project, private val syncStructure: () -> Unit) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var typeNameRenderMode : AsmTypeUtils.TypeNameRenderMode by Delegates.observable(AsmTypeUtils.TypeNameRenderMode.QUALIFIED, syncStructure())
  var methodDescriptorRenderMode : AsmMethodUtils.MethodDescriptorRenderMode by Delegates.observable(AsmMethodUtils.MethodDescriptorRenderMode.DECLARATION, syncStructure())
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
package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.FieldNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HtmlTextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode

internal class FieldStructureNode(private val field: FieldNode)
  : ValueNode(displayValue = { ctx -> "${field.name}: ${TypeUtils.toReadableName(Type.getType(field.desc).className, ctx.typeNameRenderMode)}" },
              icon = AllIcons.Nodes.Field) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    asyncAdd {
      addAccessNode(field.access, AccessGroup.FIELD)
      addAnnotationsNode("Annotations", field.visibleAnnotations, field.invisibleAnnotations)
      addAnnotationsNode("Type Annotations", field.visibleTypeAnnotations, field.invisibleTypeAnnotations)
      addAttributesNode(field.attrs)
      addInitialValueNode()
      addSignatureNode(field.signature)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun addInitialValueNode() {
    field.value?.let {
      add(HtmlTextNode("Initial value:",
                       it.toString(),
                       icon = AllIcons.Debugger.Value,
                       postFix ="<span class=\"contextHelp\">${it::class.java.simpleName}</span>"))
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
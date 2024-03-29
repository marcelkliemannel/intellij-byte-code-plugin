package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import dev.turingcomplete.intellijbytecodeplugin.bytecode.*
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Label
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.LabelNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.LocalVariableNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.*
import java.util.*
import javax.swing.Icon

internal class MethodStructureNode(private val methodNode: MethodNode, private val classNode: ClassNode)
  : ValueNode(displayValue = createDisplayValueProvider(methodNode, classNode),
              rawValue = createRawValueProvider(methodNode, classNode),
              icon = createIcon(methodNode)) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private fun createDisplayValueProvider(methodNode: MethodNode, classNode: ClassNode): (StructureTreeContext) -> String = { ctx ->
      MethodDeclarationUtils.toReadableDeclaration(methodNode.name, methodNode.desc, classNode.name, ctx.typeNameRenderMode, ctx.methodDescriptorRenderMode, true)
    }

    private fun createRawValueProvider(methodNode: MethodNode, classNode: ClassNode): (StructureTreeContext) -> String = { ctx ->
      MethodDeclarationUtils.toReadableDeclaration(methodNode.name, methodNode.desc, classNode.name, ctx.typeNameRenderMode, ctx.methodDescriptorRenderMode, false)
    }

    private fun createIcon(methodNode: MethodNode): Icon {
      return if (Access.ABSTRACT.check(methodNode.access)) AllIcons.Nodes.AbstractMethod else AllIcons.Nodes.Method
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val sortedLocalVariables : List<LocalVariableNode>? = methodNode.localVariables?.sortedBy { it.index }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    asyncAdd(true) {
      addAccessNode(methodNode.access, AccessGroup.METHOD)
      addReturnTypeNode()
      addSignatureNode(methodNode.signature)
      addAnnotationsNode("Annotations", methodNode.visibleAnnotations, methodNode.invisibleAnnotations)
      addAnnotationsNode("Type Annotations", methodNode.visibleTypeAnnotations, methodNode.invisibleTypeAnnotations)
      addMethodExceptionsNode()
      addAttributesNode(methodNode.attrs)
      addMethodParametersNode()
      if (!Access.ABSTRACT.check(methodNode.access)) {
        addMethodInstructionsNode()
        addLocalVariablesNode()
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun searchText(context: StructureTreeContext) = rawValue(context)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun addReturnTypeNode() {
    val hasReturnType = methodNode.name != "<clinit>" && methodNode.name != "<init>"
    if (!hasReturnType) {
      return
    }

    val methodType = Type.getMethodType(methodNode.desc)
    add(ValueNode("Return type:",
                  { ctx -> TypeUtils.toReadableType(methodType.returnType, ctx.typeNameRenderMode) },
                  icon = AllIcons.Actions.Rollback))
  }

  private fun addMethodExceptionsNode() {
    addTitleNodeWithElements(methodNode.exceptions, { TextNode("Exceptions", AllIcons.Nodes.ExceptionClass) }) { _, exception ->
      ValueNode(displayValue = { ctx -> TypeUtils.toReadableName(exception, ctx.typeNameRenderMode) },
                icon = AllIcons.Nodes.ExceptionClass,
                goToProvider = GoToProvider.Class(exception))
    }
  }

  private fun addMethodParametersNode() {
    val methodParameterTypes = Type.getArgumentTypes(methodNode.desc)
    if (methodParameterTypes.isEmpty()) {
      return
    }

    add(TextNode("Parameters", AllIcons.Nodes.Parameter).apply {
      val namesToAccess = collectMethodParametersNameToAccess(methodParameterTypes)
      methodParameterTypes.mapIndexed { index, type ->
        val nameToAccess = namesToAccess.getOrNull(index)
        val name = nameToAccess?.first ?: "var$index"
        add(ValueNode(displayValue = { ctx -> "$name: ${TypeUtils.toReadableType(type, ctx.typeNameRenderMode)}" }, icon = AllIcons.Nodes.Parameter).apply {
          nameToAccess?.second?.let { access ->
            addAccessNode(access, AccessGroup.PARAMETER)
          }
          addAnnotationsNode("Annotations", methodNode.visibleParameterAnnotations?.get(index), methodNode.invisibleParameterAnnotations?.get(index))
        })
      }
    })
  }

  private fun collectMethodParametersNameToAccess(methodParameterTypes: Array<Type>): List<Pair<String?, Int>> {
    return if (methodNode.parameters != null) {
      methodNode.parameters.map { parameter -> parameter.name to parameter.access }
    }
    else if (sortedLocalVariables != null) {
      // Get name from local variables
      val offset = if (Access.STATIC.check(methodNode.access)) 0 else 1
      IntRange(offset, (methodParameterTypes.size - 1) + offset).map { i ->
        sortedLocalVariables.elementAtOrNull(i)?.name to 0
      }.toList()
    }
    else {
      return listOf()
    }
  }

  private fun addMethodInstructionsNode() {
    methodNode.instructions ?: return

    add(TextNode("Instructions", AllIcons.Actions.ListFiles).apply {
      asyncAdd(true) {
        val (labelNames, methodFrames) = MethodFramesUtils.collectFrames(methodNode, classNode)
        methodFrames.forEach { methodFrame ->
          val postFix = when {
            (methodFrame.instruction.opcode >= 0) -> "<span class=\"contextHelp\">Opcode: ${methodFrame.instruction.opcode}</span>"
            (methodFrame.instruction is LabelNode) -> "<span class=\"contextHelp\">Label</span>"
            else -> null
          }
          add(HtmlTextNode(displayValue = methodFrame.textifiedInstruction,
                           rawValue = methodFrame.textifiedInstruction.trim(),
                           postFix = postFix))
        }

        add(ShowFramesNode(methodFrames, methodNode))

        addTryCatchBlockNode(labelNames)

        add(TextNode("Max locals: ${methodNode.maxLocals}; stack: ${methodNode.maxStack}", icon = AllIcons.General.Information))
      }
    })
  }

  private fun addLocalVariablesNode() {
    addTitleNodeWithElements(sortedLocalVariables, { TextNode("Local Variables", AllIcons.Nodes.Variable) }) { index, localVariable ->
      ValueNode(displayValue = { ctx -> "#${localVariable.index} ${localVariable.name}: ${TypeUtils.toReadableType(localVariable.desc, ctx.typeNameRenderMode)}" },
                icon = AllIcons.Nodes.Variable).apply {
        asyncAdd {
          addSignatureNode(localVariable.signature)

          val visibleLocalVariableAnnotations = methodNode.visibleLocalVariableAnnotations
                  ?.flatMap { it.index.map { i -> i to it } }
                  ?.groupBy({ it.first }, { it.second })
          val invisibleLocalVariableAnnotations = methodNode.invisibleLocalVariableAnnotations
                  ?.flatMap { it.index.map { i -> i to it } }
                  ?.groupBy({ it.first }, { it.second })

          addAnnotationsNode("Type Annotations",
                             visibleLocalVariableAnnotations?.getOrDefault(index, null),
                             invisibleLocalVariableAnnotations?.getOrDefault(index, null))
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ShowFramesNode(private val methodFrames: List<MethodFramesUtils.MethodFrame>,
                               private val methodNode: MethodNode) : HyperLinkNode("Show frames") {
    init {
      addHyperLinkListener { _, ctx ->
        FramesDialog(methodNode, ctx.typeNameRenderMode, methodFrames, ctx.project).show()
      }
    }
  }

  private fun StructureNode.addTryCatchBlockNode(labelNames: Map<Label, String>) {
    val tryCatchBlocks = methodNode.tryCatchBlocks
    if (tryCatchBlocks.isEmpty()) {
      return
    }

    addTitleNodeWithElements(tryCatchBlocks, { TextNode("Try Catch Blocks") }, true) { _, tryCatchBlock ->
      val startLabelName = labelNames.getOrDefault(tryCatchBlock.start.label, "unknown")
      val endLabelName = labelNames.getOrDefault(tryCatchBlock.end.label, "unknown")

      var postFix = "from $startLabelName to $endLabelName"
      tryCatchBlock.handler?.let {
        postFix += ", handled in ${labelNames.getOrDefault(it.label, "unknown")}"
      }

      if (tryCatchBlock.type != null) {
        ValueNode(displayValue = { ctx -> TypeUtils.toReadableName(tryCatchBlock.type, ctx.typeNameRenderMode) },
                  postFix = postFix,
                  goToProvider = GoToProvider.Class(tryCatchBlock.type))
      }
      else {
        TextNode(postFix.replaceFirstChar { it.titlecase(Locale.getDefault()) })
      }
    }
  }
}

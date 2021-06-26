package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.popup.JBPopupFactory
import dev.turingcomplete.intellijbytecodeplugin.bytecode.*
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Label
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.LabelNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.*
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HyperLinkNode.HyperLinkListener
import java.util.*
import java.util.stream.IntStream
import kotlin.streams.toList

internal class MethodStructureNode(private val methodNode: MethodNode, private val classNode: ClassNode)
  : ValueNode(displayValue = { ctx -> MethodDeclarationUtils.toReadableDeclaration(methodNode.name, methodNode.desc, classNode.name, ctx.typeNameRenderMode, ctx.methodDescriptorRenderMode, true) },
              rawValue = { ctx -> MethodDeclarationUtils.toReadableDeclaration(methodNode.name, methodNode.desc, classNode.name, ctx.typeNameRenderMode, ctx.methodDescriptorRenderMode, false) },
              icon = AllIcons.Nodes.Method) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val sortedLocalVariables = methodNode.localVariables?.sortedBy { it.index }

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
      addMethodInstructionsNode()
      addLocalVariablesNode()
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

    val nameToAccess = collectMethodParametersNameToAccess(methodParameterTypes)
    add(TextNode("Parameters", AllIcons.Nodes.Parameter).apply {
      nameToAccess.mapIndexed { index, nameToAccess ->
        val valueNode = if (index < methodParameterTypes.size) {
          ValueNode(displayValue = { ctx -> "${nameToAccess.first}: ${TypeUtils.toReadableType(methodParameterTypes[index], ctx.typeNameRenderMode)}" }, icon = AllIcons.Nodes.Parameter)
        }
        else {
          ValueNode(displayValue = nameToAccess.first, icon = AllIcons.Nodes.Parameter)
        }
        add(valueNode.apply {
          addAccessNode(nameToAccess.second, AccessGroup.PARAMETER)
          addAnnotationsNode("Annotations", methodNode.visibleParameterAnnotations?.get(index), methodNode.invisibleParameterAnnotations?.get(index))
        })
      }
    })
  }

  private fun collectMethodParametersNameToAccess(methodParameterTypes: Array<Type>): List<Pair<String, Int>> {
    return if (methodNode.parameters != null) {
      methodNode.parameters.let { parameters ->
        parameters.map { parameter -> parameter.name to parameter.access }
      }
    }
    else if (sortedLocalVariables != null) {
      // Get name from local variables
      val offset = if (Access.STATIC.check(methodNode.access)) 0 else 1
      IntStream.range(offset, methodParameterTypes.size + offset).mapToObj { i ->
        (sortedLocalVariables.elementAtOrNull(i)?.name ?: "<i>unknown</i>") to 0
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
      addHyperLinkListener(createHyperLinkListener())
    }

    private fun createHyperLinkListener() = HyperLinkListener { _, ctx ->
      val stacksAndLocalsPanel = FramesPanel(ctx.typeNameRenderMode, methodFrames)
      JBPopupFactory.getInstance()
              .createComponentPopupBuilder(stacksAndLocalsPanel, stacksAndLocalsPanel)
              .setRequestFocus(true)
              .setTitle("Frames of method '${methodNode.name}${methodNode.desc}'")
              .setFocusable(true)
              .setResizable(true)
              .setMovable(true)
              .setModalContext(false)
              .addUserData("SIMPLE_WINDOW")
              .setCancelKeyEnabled(true)
              .setCancelOnClickOutside(true)
              .setCancelOnOtherWindowOpen(true)
              .createPopup()
              .showCenteredInCurrentWindow(ctx.project)
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
        postFix += ", handled in ${labelNames.getOrDefault(it.label, "unknown") }"
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

package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import dev.turingcomplete.intellijbytecodeplugin._ui.configureForCell
import dev.turingcomplete.intellijbytecodeplugin.bytecode.*
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Label
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.LabelNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.RenderOption
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.SearchProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.*
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HyperLinkNode.HyperLinkListener
import java.awt.Component
import java.awt.Dimension
import java.util.*
import java.util.stream.IntStream
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import kotlin.math.max
import kotlin.properties.Delegates
import kotlin.reflect.KProperty
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
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun addMethodExceptionsNode() {
    addTitleNodeWithElements(methodNode.exceptions, { TextNode("Exceptions", AllIcons.Nodes.ExceptionClass) }) { _, exception ->
      ValueNode(displayValue = { ctx -> TypeUtils.toReadableName(exception, ctx.typeNameRenderMode) },
                icon = AllIcons.Nodes.ExceptionClass,
                searchProvider = SearchProvider.Class(exception))
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
                  searchProvider = SearchProvider.Class(tryCatchBlock.type))
      }
      else {
        TextNode(postFix.replaceFirstChar { it.titlecase(Locale.getDefault()) })
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class FramesPanel(initialTypeNameRenderMode: TypeUtils.TypeNameRenderMode, methodFrames: List<MethodFramesUtils.MethodFrame>)
    : SimpleToolWindowPanel(false, true) {

    private val stacksAndLocalsModel = FramesModel(initialTypeNameRenderMode, methodFrames)
    private val table = JBTable(stacksAndLocalsModel).apply {
      setDefaultRenderer(String::class.java, FramesCellRenderer())
    }

    init {
      toolbar = createToolbar()
      setContent(ScrollPaneFactory.createScrollPane(table, true))
      preferredSize = Dimension(700, 250)
    }

    private fun createToolbar(): JComponent {
      val toolbarGroup = DefaultActionGroup(createRenderOptionsActionGroup(),
                                            createShowMultipleValuesVerticallyToggleAction())
      return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarGroup, false).component
    }

    private fun createRenderOptionsActionGroup(): DefaultActionGroup {
      return object : DefaultActionGroup("Render Options", true), Toggleable, DumbAware {

        init {
          templatePresentation.icon = AllIcons.Actions.Edit

          TypeUtils.TypeNameRenderMode.values().forEach {
            add(RenderOption(it.title, { stacksAndLocalsModel.typeNameRenderMode = it; }, { stacksAndLocalsModel.typeNameRenderMode == it }))
          }
        }
      }
    }

    private fun createShowMultipleValuesVerticallyToggleAction(): ToggleAction {
      return object : DumbAwareToggleAction("Show Multiple Values in Multiple Rows", null, AllIcons.Actions.SplitHorizontally) {

        override fun isSelected(e: AnActionEvent): Boolean = stacksAndLocalsModel.frameAsMultipleRows

        override fun setSelected(e: AnActionEvent, state: Boolean) {
          stacksAndLocalsModel.apply {
            frameAsMultipleRows = state
            fireTableDataChanged()
          }
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class FramesModel(initialTypeNameRenderMode: TypeUtils.TypeNameRenderMode,
                            private val frames: List<MethodFramesUtils.MethodFrame>) : AbstractTableModel() {

    var typeNameRenderMode: TypeUtils.TypeNameRenderMode by Delegates.observable(initialTypeNameRenderMode, rebuildData())
    var frameAsMultipleRows: Boolean by Delegates.observable(true, rebuildData())

    private lateinit var data: Array<Array<String>>

    init {
      buildData()
    }

    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String {
      return when (column) {
        0 -> "Instruction"
        1 -> "Stack"
        2 -> "Locals"
        else -> throw IllegalArgumentException("Unknown column index '$column'.")
      }
    }

    override fun getRowCount(): Int = data.size

    override fun getColumnClass(column: Int): Class<*> = String::class.java

    override fun getValueAt(row: Int, column: Int): Any = data[row][column]

    private fun buildData() {
      data = if (frameAsMultipleRows) collectFrameAsMultipleRows() else collectFrameAsSingleRow()
      fireTableDataChanged()
    }

    private fun <T> rebuildData(): (property: KProperty<*>, oldValue: T, newValue: T) -> Unit = { _, old, new ->
      if (old != new) {
        buildData()
      }
    }

    private fun collectFrameAsSingleRow(): Array<Array<String>> {
      return frames.map { frame ->
        val locals = frame.locals.joinToString(" | ") { local ->
          TypeUtils.toReadableType(local, typeNameRenderMode)
        }
        val stackElements = frame.stack.joinToString(" | ") { stackElement ->
          TypeUtils.toReadableType(stackElement, typeNameRenderMode)
        }
        arrayOf(frame.textifiedInstruction, locals, stackElements)
      }.toTypedArray()
    }

    private fun collectFrameAsMultipleRows(): Array<Array<String>> {
      return frames.map { frame ->
        val maxRows = max(1, max(frame.locals.size, frame.stack.size))
        val frameData = Array(maxRows) { Array(3) { "" } }
        frameData[0][0] = frame.textifiedInstruction
        frame.locals.forEachIndexed { localIndex, local ->
          frameData[localIndex][1] = TypeUtils.toReadableType(local, typeNameRenderMode)
        }
        frame.stack.forEachIndexed { stackIndex, stackElement ->
          frameData[stackIndex][2] = TypeUtils.toReadableType(stackElement, typeNameRenderMode)
        }
        frameData
      }.reduceOrNull { acc, unit -> acc.plus(unit) } ?: arrayOf()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class FramesCellRenderer : JBLabel(), TableCellRenderer {

    override fun getTableCellRendererComponent(table: JTable, value: Any, selected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
      text = value as String
      return this.configureForCell(table, selected, hasFocus)
    }
  }
}

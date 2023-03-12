package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBLabel
import com.intellij.ui.table.JBTable
import dev.turingcomplete.intellijbytecodeplugin._ui.ByteCodeToolWindowFactory
import dev.turingcomplete.intellijbytecodeplugin._ui.CopyValueAction
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils.Table.getSingleSelectedValue
import dev.turingcomplete.intellijbytecodeplugin._ui.ViewValueAction
import dev.turingcomplete.intellijbytecodeplugin._ui.configureForCell
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodFramesUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.RenderOption
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import kotlin.math.max
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class FramesPanel(initialTypeNameRenderMode: TypeUtils.TypeNameRenderMode, methodFrames: List<MethodFramesUtils.MethodFrame>)
  : SimpleToolWindowPanel(false, true), DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val stacksAndLocalsModel = FramesModel(initialTypeNameRenderMode, methodFrames)
  private val table: JBTable = createFramesTable()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar(this)
    setContent(ScrollPaneFactory.createScrollPane(table, true))
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getData(dataId: String): Any? {
    return when {
      CommonDataKeys.VALUE.`is`(dataId) -> getSingleSelectedValue(table)?.takeIf { it.isNotBlank() }
      else -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(targetComponent: JComponent): JComponent {
    val toolbarGroup = DefaultActionGroup(
      createRenderOptionsActionGroup(),
      createShowMultipleValuesVerticallyToggleAction()
    )
    return ActionManager.getInstance().createActionToolbar("${ByteCodeToolWindowFactory.TOOLBAR_PLACE_PREFIX}.methodFrames", toolbarGroup, false).run {
      setTargetComponent(targetComponent)
      component
    }
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

      override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }
  }

  private fun createFramesTable(): JBTable {
    return JBTable(stacksAndLocalsModel).apply {
      setDefaultRenderer(String::class.java, FramesCellRenderer())
      addMouseListener(UiUtils.Table.createContextMenuMouseListener(
        FramesPanel::class.java.simpleName
      ) {
        DefaultActionGroup().apply {
          add(CopyValueAction())
          add(ViewValueAction())
        }
      })
      TableSpeedSearch(this)
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
        1 -> "Locals"
        2 -> "Stack"
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
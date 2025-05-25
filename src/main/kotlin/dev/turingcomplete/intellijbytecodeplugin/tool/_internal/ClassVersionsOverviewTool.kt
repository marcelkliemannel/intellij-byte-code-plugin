package dev.turingcomplete.intellijbytecodeplugin.tool._internal

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper.IdeModalityType
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijbytecodeplugin._ui.CopyValueAction
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.ViewValueAction
import dev.turingcomplete.intellijbytecodeplugin.bytecode.ClassVersionUtils
import dev.turingcomplete.intellijbytecodeplugin.common.CommonDataKeys
import dev.turingcomplete.intellijbytecodeplugin.tool.ByteCodeTool
import java.awt.Dimension
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

class ClassVersionsOverviewTool : ByteCodeTool("Class Versions Overview") {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun execute(project: Project?) {
    if (project?.isDisposed == true) {
      return
    }

    UiUtils.Dialog.show("Class Versions Overview", BorderLayoutPanel().apply {
      val model = DefaultTableModel(
        ClassVersionUtils.CLASS_VERSIONS.map { arrayOf(it.specification, it.major) }.toTypedArray(),
        arrayOf("Specification", "Class Version")
      )
      val table = ClassVersionsTable(model)
      addToCenter(ScrollPaneFactory.createScrollPane(table))
    }, Dimension(400, 500), project, IdeModalityType.MODELESS)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class ClassVersionsTable(tableModel: TableModel): JBTable(tableModel), DataProvider {

    init {
      addMouseListener(UiUtils.Table.createContextMenuMouseListener(ClassVersionsOverviewTool::class.java.simpleName) {
        DefaultActionGroup().apply {
          add(CopyValueAction())
          add(ViewValueAction())
        }
      })
    }

    override fun getData(dataId: String): Any? = when {
      CommonDataKeys.VALUE.`is`(dataId) -> UiUtils.Table.getSingleSelectedValue(this)
      else -> null
    }
  }
}

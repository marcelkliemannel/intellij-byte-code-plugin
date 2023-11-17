package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.compiler.impl.CompositeScope
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileScope
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin._ui.joinAsNaturalLanguage
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import java.nio.file.Path
import javax.swing.Action
import javax.swing.Icon
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
internal class ClassFilesPreparationService(private val project: Project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  private val compilerManager by lazy { CompilerManager.getInstance(project) }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun openClassFilesAfterPreparation(
    classFilePreparationTasks: List<ClassFilePreparationTask>,
    parentComponent: JComponent? = null,
    openClassFile: (ProcessableClassFile) -> Unit,
  ) {
    if (classFilePreparationTasks.isEmpty()) {
      return
    }

    val outdatedClassFiles = mutableListOf<ClassFilePreparationTask>()
    val missingClassFiles = mutableListOf<ClassFilePreparationTask>()

    classFilePreparationTasks.forEach { classFilePreparationContext ->
      val classFile = VirtualFileManager.getInstance().findFileByNioPath(classFilePreparationContext.expectedClassFilePath)
      if (classFile == null) {
        missingClassFiles.add(classFilePreparationContext)
        return@forEach
      }

      classFile.refresh(false, false)
      if (!classFile.isValid) {
        missingClassFiles.add(classFilePreparationContext)
        return@forEach
      }

      val sourceFile = classFilePreparationContext.sourceFile
      sourceFile.file.refresh(false, false)
      if (!sourceFile.file.isValid) {
        logger.warn("Can't process source file '${sourceFile.file.path}' since it is no longer valid")
        return@forEach
      }
      if (!compilerManager.isUpToDate(compilerManager.createFilesCompileScope(arrayOf(sourceFile.file)))) {
        outdatedClassFiles.add(classFilePreparationContext)
      }
      else {
        openClassFile(ProcessableClassFile(classFile, sourceFile))
      }
    }

    val compileScope = sequenceOf(
      determineCompileScope(outdatedClassFiles, PrepareReason.OUT_DATED, parentComponent, openClassFile),
      determineCompileScope(missingClassFiles, PrepareReason.MISSING, parentComponent, openClassFile)
    ).filterNotNull().reduceOrNull { first, second -> CompositeScope(first, second) }

    if (compileScope != null) {
      compilerManager.compile(compileScope, OpenClassFilesAfterCompilationHandler(outdatedClassFiles.plus(missingClassFiles), openClassFile))
    }
  }

  private fun determineCompileScope(
    classFiles: MutableList<ClassFilePreparationTask>,
    prepareReason: PrepareReason,
    parentComponent: JComponent?,
    openClassFile: (ProcessableClassFile) -> Unit,
  ): CompileScope? {
    if (classFiles.isEmpty()) {
      return null
    }

    val selectedPrepareMode = PrepareClassFilesOptionsDialog(project, prepareReason, classFiles, parentComponent)
      .showAndGetResult()
    return when (selectedPrepareMode) {
      PrepareMode.BUILD_PROJECT -> CompilerManager.getInstance(project).createProjectCompileScope(project)
      PrepareMode.COMPILE_MODULES -> CompilerManager.getInstance(project)
        .createModulesCompileScope(classFiles.map { it.sourceFile.module }.toTypedArray(), false)

      PrepareMode.COMPILE_MODULE_TREES -> CompilerManager.getInstance(project)
        .createModulesCompileScope(classFiles.map { it.sourceFile.module }.toTypedArray(), true)

      PrepareMode.COMPILE_FILES -> CompilerManager.getInstance(project).createFilesCompileScope(classFiles.map { it.sourceFile.file }.toTypedArray())
      PrepareMode.USE_DIRECTLY -> {
        assert(prepareReason == PrepareReason.OUT_DATED)
        classFiles.forEach {
          val classFile = VirtualFileManager.getInstance().findFileByNioPath(it.expectedClassFilePath)
          if (classFile != null) {
            openClassFile(ProcessableClassFile(classFile, it.sourceFile))
          }
        }
        null
      }

      null -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal class ClassFilePreparationTask(val expectedClassFilePath: Path, val sourceFile: SourceFile)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenClassFilesAfterCompilationHandler(
    private val classFilesNeedingPreparation: List<ClassFilePreparationTask>,
    private val openClassFile: (ProcessableClassFile) -> Unit
  ) : CompileStatusNotification {

    override fun finished(aborted: Boolean, errors: Int, warnings: Int, compileContext: CompileContext) {
      if (aborted || errors > 0) {
        return
      }

      if (compileContext.project.isDisposed) {
        return
      }

      ApplicationManager.getApplication().executeOnPooledThread {
        val virtualFileManager = VirtualFileManager.getInstance()
        classFilesNeedingPreparation.forEach {
          val classFile = virtualFileManager.refreshAndFindFileByNioPath(it.expectedClassFilePath)
          if (classFile != null && classFile.isValid) {
            openClassFile(ProcessableClassFile(classFile, it.sourceFile))
          }
          else {
            logger.warn("Failed to find class file '${it.expectedClassFilePath}' after compilation")
          }
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal enum class PrepareMode(
    val code: Int,
    val singularTitle: String,
    val pluralTitle: String = singularTitle,
    val icon: Icon = AllIcons.Actions.Compile
  ) {

    BUILD_PROJECT(code = 2, singularTitle = "Build &Project"),
    COMPILE_MODULES(code = 3, singularTitle = "Compile &Modules"),
    COMPILE_MODULE_TREES(code = 4, singularTitle = "Compile Module and &Dependent Modules"),
    COMPILE_FILES(code = 5, singularTitle = "Compile F&ile", pluralTitle = "Compile &Files"),
    USE_DIRECTLY(code = 6, singularTitle = "Use file as it is", pluralTitle = "Use files as they are", icon = AllIcons.Actions.Execute)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal enum class PrepareReason(val singularQuestion: (ClassFilePreparationTask) -> String,
                                    val pluralQuestion: (List<ClassFilePreparationTask>) -> String,
                                    val allowsUseDirectly: Boolean) {

    OUT_DATED(
      { classFile -> "The class file for the source file '${classFile.sourceFile.file.name}' is outdated. Should it be compiled?" },
      { classFiles -> "The class files for the source files ${classFiles.joinAsNaturalLanguage { "'${it.sourceFile.file.name}'" }} are outdated. Should they be compiled?" },
      true
    ),
    MISSING(
      { classFile -> "The class file for the source file '${classFile.sourceFile.file.name}' is missing. Should it be compiled?" },
      { classFiles -> "The class files for the source files ${classFiles.joinAsNaturalLanguage { "'${it.sourceFile.file.name}'" }}' are missing. Should they be compiled?" },
      false
    )
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal class PrepareClassFilesOptionsDialog(
    project: Project,
    private val prepareReason: PrepareReason,
    private val classFiles: List<ClassFilePreparationTask>,
    parentComponent: JComponent? = null
  ) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE, true) {

    private val useSingular = classFiles.size == 1

    init {
      myOKAction = if (prepareReason.allowsUseDirectly) {
        createPrepareModeAction(PrepareMode.USE_DIRECTLY)
      }
      else {
        createPrepareModeAction(PrepareMode.COMPILE_FILES)
      }
      myOKAction.apply {
        putValue(DEFAULT_ACTION, java.lang.Boolean.TRUE)
      }

      title = if (useSingular) "Analyse Class File" else "Analyse Class Files"
      isResizable = false
      super.init()
    }

    override fun createCenterPanel(): JComponent = panel {
      row {
        label(if (useSingular) prepareReason.singularQuestion(classFiles[0]) else prepareReason.pluralQuestion(classFiles))
      }
    }

    override fun createActions(): Array<Action> {
      val actions = mutableListOf(cancelAction)

      val nestedCompileOptions = arrayOf(
        createPrepareModeAction(PrepareMode.COMPILE_MODULES),
        createPrepareModeAction(PrepareMode.COMPILE_MODULE_TREES)
      )
      val buildProjectActionTitle = if (useSingular) PrepareMode.BUILD_PROJECT.singularTitle else PrepareMode.BUILD_PROJECT.pluralTitle
      actions.add(UiUtils.createOptionsAction(buildProjectActionTitle, AllIcons.Actions.Compile, nestedCompileOptions) {
        close(PrepareMode.BUILD_PROJECT.code, true)
      })

      if (prepareReason.allowsUseDirectly) {
        actions.add(createPrepareModeAction(PrepareMode.COMPILE_FILES))
      }

      actions.add(okAction)

      return actions.toTypedArray()
    }

    override fun isOK(): Boolean = PrepareMode.values().find { it.code == exitCode } != null

    fun showAndGetResult(): PrepareMode? {
      if (!showAndGet()) {
        return null
      }

      return PrepareMode.values().find { it.code == exitCode }
    }

    private fun createPrepareModeAction(prepareMode: PrepareMode): Action {
      val title = if (useSingular) prepareMode.singularTitle else prepareMode.pluralTitle
      return UiUtils.createAction(title, prepareMode.icon) {
        close(prepareMode.code, true)
      }
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val logger = Logger.getInstance(OpenClassFilesTask::class.java)
  }
}
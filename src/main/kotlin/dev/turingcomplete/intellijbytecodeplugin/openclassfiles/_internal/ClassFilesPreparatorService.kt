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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.dsl.builder.panel
import dev.turingcomplete.intellijbytecodeplugin._ui.UiUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile.CompilableSourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates.AbsoluteClassFileCandidates
import org.jsoup.internal.StringUtil.StringJoiner
import javax.swing.Action
import javax.swing.Icon
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
internal class ClassFilesPreparatorService(private val project: Project) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  private val compilerManager by lazy { CompilerManager.getInstance(project) }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun prepareClassFiles(
    preparationTasks: List<ClassFilePreparationTask>,
    parentComponent: JComponent? = null,
    consumeClassFile: (ClassFile) -> Unit,
  ) {
    if (preparationTasks.isEmpty()) {
      return
    }

    val outdatedClassFiles = mutableListOf<ClassFilePreparationTask>()
    val missingClassFiles = mutableListOf<ClassFilePreparationTask>()

    preparationTasks.forEach { classFilePreparationContext ->
      val classFile = VirtualFileManager.getInstance().findFileByNioPath(classFilePreparationContext.compilerOutputClassFileCandidates.primaryPath)
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
        consumeClassFile(ClassFile(classFile, sourceFile))
      }
    }

    val compileScope = sequenceOf(
      determineCompileScope(outdatedClassFiles, PrepareReason.OUT_DATED, parentComponent, consumeClassFile),
      determineCompileScope(missingClassFiles, PrepareReason.MISSING, parentComponent, consumeClassFile)
    ).filterNotNull().reduceOrNull { first, second -> CompositeScope(first, second) }

    if (compileScope != null) {
      compilerManager.compile(compileScope, OpenClassFilesAfterCompilationHandler(outdatedClassFiles.plus(missingClassFiles), consumeClassFile))
    }
  }

  private fun determineCompileScope(
    classFiles: MutableList<ClassFilePreparationTask>,
    prepareReason: PrepareReason,
    parentComponent: JComponent?,
    openClassFile: (ClassFile) -> Unit,
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
          val classFile = VirtualFileManager.getInstance().findFileByNioPath(it.compilerOutputClassFileCandidates.primaryPath)
          if (classFile != null) {
            openClassFile(ClassFile(classFile, it.sourceFile))
          }
        }
        null
      }

      null -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal data class ClassFilePreparationTask(
    val compilerOutputClassFileCandidates: AbsoluteClassFileCandidates,
    val sourceFile: CompilableSourceFile
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenClassFilesAfterCompilationHandler(
    private val classFilesNeedingPreparation: List<ClassFilePreparationTask>,
    private val openClassFile: (ClassFile) -> Unit
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
        classFilesNeedingPreparation.forEach { task ->
          val classVirtualFile = task.compilerOutputClassFileCandidates
            .allPaths()
            .firstNotNullOfOrNull { path ->
              virtualFileManager.refreshAndFindFileByNioPath(path)?.takeIf { it.isValid }
            }
          if (classVirtualFile != null) {
            openClassFile(ClassFile(classVirtualFile, task.sourceFile))
          }
          else {
            ApplicationManager.getApplication().invokeLater {
              val errorMessage = task.compilerOutputClassFileCandidates
                .formatNotFoundError("cannot be found in the compiler output directory of module '${task.sourceFile.module.name}'.", compileContext.project)
              Messages.showErrorDialog(compileContext.project, errorMessage, "Analyse Class Files")
            }
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

    // Code `1` is fix for the cancel action
    BUILD_PROJECT(code = 2, singularTitle = "Build Project"),
    COMPILE_MODULES(code = 3, singularTitle = "Compile Module"),
    COMPILE_MODULE_TREES(code = 4, singularTitle = "Compile Module and Dependent Modules"),
    COMPILE_FILES(code = 5, singularTitle = "Compile File", pluralTitle = "Compile Files"),
    USE_DIRECTLY(code = 6, singularTitle = "Use file as it is", pluralTitle = "Use files as they are", icon = AllIcons.Actions.Execute)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal enum class PrepareReason(
    val question: (Map<String, List<String>>) -> String,
    val allowsUseDirectly: Boolean
  ) {

    OUT_DATED(
      { sourceFileNamesToClassFilesNames ->
        val sourceFileNames = sourceFileNamesToClassFilesNames.keys.toList()
        val classFilesNames = sourceFileNamesToClassFilesNames.values.flatten().toList()
        StringJoiner(" ").apply {
          add("<html>")
          add("The")
          add("source file${if (sourceFileNames.size > 1) "s" else ""}")
          add(formatFileNames(sourceFileNames))
          add(if (classFilesNames.size > 1) "are" else "is")
          add("outdated. Should")
          add(if (sourceFileNames.size > 1) "they" else "it")
          add("be compiled?")
          add("</html>")
        }.complete()
      },
      true
    ),
    MISSING(
      { sourceFileNamesToClassFilesNames ->
        val sourceFileNames = sourceFileNamesToClassFilesNames.keys.toList()
        val classFilesNames = sourceFileNamesToClassFilesNames.values.flatten().toList()
        StringJoiner(" ").apply {
          add("<html>")
          add("The")
          add("class file${if (classFilesNames.size > 1) "s" else ""}")
          add(formatFileNames(classFilesNames))
          add("for the")
          add("source file${if (sourceFileNames.size > 1) "s" else ""}")
          add(formatFileNames(sourceFileNames))
          add(if (classFilesNames.size > 1) "are" else "is")
          add("missing. Should")
          add(if (sourceFileNames.size > 1) "they" else "it")
          add("be compiled?")
          add("</html>")
        }.complete()
      },
      false
    )
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal class PrepareClassFilesOptionsDialog(
    project: Project,
    private val prepareReason: PrepareReason,
    classFiles: List<ClassFilePreparationTask>,
    parentComponent: JComponent? = null
  ) : DialogWrapper(project, parentComponent, false, IdeModalityType.IDE, true) {

    // There could be multiple `ClassFilePreparationTask`s for the same source
    // file. For example, this could be the case for a Kotlin file with two
    // top-level classes.
    private val sourceFileNamesToClassFilesNames =
      classFiles.groupBy({ it.sourceFile.file.name }) { it.compilerOutputClassFileCandidates.primaryPath.fileName.toString() }
    private val useSingular = sourceFileNamesToClassFilesNames.size == 1

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
        label(prepareReason.question(sourceFileNamesToClassFilesNames))
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

    override fun isOK(): Boolean = PrepareMode.entries.find { it.code == exitCode } != null

    fun showAndGetResult(): PrepareMode? {
      if (!showAndGet()) {
        return null
      }

      return PrepareMode.entries.find { it.code == exitCode }
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

    private val logger = Logger.getInstance(ClassFilesFinderService::class.java)

    fun formatFileNames(fileNames: List<String>): String =
      if (fileNames.size == 1) {
        "'${fileNames[0]}'"
      }
      else {
        fileNames.joinToString(prefix = "<ul>", postfix = "</ul>", separator = "") { "<li>$it</li>" }
      }
  }
}
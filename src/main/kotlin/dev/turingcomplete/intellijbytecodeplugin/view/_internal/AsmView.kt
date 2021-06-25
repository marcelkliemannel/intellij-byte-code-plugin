package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TraceUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.ASMifier
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView

class AsmView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "ASM", AllIcons.FileTypes.Text, METHOD_LINE_REGEX) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    // Example: "methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);"
    private val METHOD_LINE_REGEX = Regex("^.*classWriter\\.visitMethod\\(.*?,\\s\"(?<name>.+?)\".*\$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun parseByteCode(parsingOptions: Int): String {
    return TraceUtils.traceVisit(classFileContext.classReader(), parsingOptions, ASMifier())
  }

  override fun openInEditorFileName() = "${classFileContext.classFile().nameWithoutExtension}Dump.java"

  override fun additionalToolBarActions(): ActionGroup {
    return DefaultActionGroup().apply {
      add(ReformatCodeAction())
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private inner class ReformatCodeAction : DumbAwareAction("Reformat Code", null, AllIcons.Actions.PrettyPrint) {

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = getText() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
      val text = getText() ?: return
      createPsiFile(text)?.let { psiFile ->
        formatCode(psiFile) {
          setTextASM(it)
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = AsmView(classFileContext)
  }
}
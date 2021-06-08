package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
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

  /**
   * It would be nicer if this was done directly when parsing (because the ASM
   * code looks really ugly), but unfortunately the reformat is executed in the
   * EDT, which results in a noticeable freeze the loading of the tab.
   */
  private inner class ReformatCodeAction : DumbAwareAction("Reformat Code", null, AllIcons.Actions.PrettyPrint) {

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = getText() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
      val text = getText() ?: return
      createPsiFile(text)?.let { psiFile ->
        formatCode(psiFile) {
          setText(it)
        }
      }
    }

    private fun createPsiFile(asmifiedText: String): PsiFile? {
      return ApplicationManager.getApplication().runReadAction(Computable<PsiFile?> {
        val lightVirtualFile = LightVirtualFile(openInEditorFileName(), JavaFileType.INSTANCE, asmifiedText)
        PsiManager.getInstance(classFileContext.project()).findFile(lightVirtualFile)
      })
    }

    private fun formatCode(psiFile: PsiFile, onSuccess: (String) -> Unit) {
      val reformatProcessor = ReformatCodeProcessor(classFileContext.project(), psiFile, null, false)
      val rearrangeProcessor = RearrangeCodeProcessor(reformatProcessor)
      rearrangeProcessor.setPostRunnable { onSuccess(psiFile.text) }
      rearrangeProcessor.run()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = AsmView(classFileContext)
  }
}
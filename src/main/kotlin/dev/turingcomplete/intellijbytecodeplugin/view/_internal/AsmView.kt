package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.icons.AllIcons
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TraceUtils.traceVisit
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.ASMifier
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView

class AsmView(classFileContext: ClassFileContext) :
  ByteCodeParsingResultView(classFileContext, "ASM", AllIcons.FileTypes.Text, METHOD_LINE_REGEX) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    // Example: "methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);"
    private val METHOD_LINE_REGEX = Regex("^.*classWriter\\.visitMethod\\(.*?,\\s\"(?<name>.+?)\".*\$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun asyncParseByteCode(parsingOptions: Int, onSuccess: (String) -> Unit) {
    val asmifiedText = traceVisit(classFileContext.classReader(), parsingOptions, ASMifier())

    val asmifiedTextPsiFile = ApplicationManager.getApplication().runReadAction(Computable<PsiFile?> {
      val lightVirtualFile = LightVirtualFile(openInEditorFileName(), JavaFileType.INSTANCE, asmifiedText)
      PsiManager.getInstance(classFileContext.project()).findFile(lightVirtualFile)
    })

    if (asmifiedTextPsiFile != null) {
      ApplicationManager.getApplication().invokeAndWait {
        val reformatProcessor = ReformatCodeProcessor(classFileContext.project(), asmifiedTextPsiFile, null, false)
        val rearrangeProcessor = RearrangeCodeProcessor(reformatProcessor)
        rearrangeProcessor.setPostRunnable { onSuccess(asmifiedTextPsiFile.text) }
        rearrangeProcessor.run()
      }
    }
    else {
      onSuccess(asmifiedText)
    }
  }

  override fun openInEditorFileName() = "${classFileContext.classFile().nameWithoutExtension}Dump.java"

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = AsmView(classFileContext)
  }
}
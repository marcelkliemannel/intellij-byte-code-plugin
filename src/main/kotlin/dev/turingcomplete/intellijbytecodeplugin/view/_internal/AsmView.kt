package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = AsmView(classFileContext)
  }
}
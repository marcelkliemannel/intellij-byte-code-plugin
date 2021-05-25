package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTraceUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Textifier
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView


class TextifiedView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "Textified", AllIcons.FileTypes.JavaClass, METHOD_LINE_REGEX) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    // Example: "  private <init>(Ljava/lang/ClassLoader;Ljava/lang/Class;)V"
    private val METHOD_LINE_REGEX = Regex("^\\s\\s(?:[^/\\s]+\\s)*(?<name>(\\w|\\\$|_)[^\\s(]+)\\(.*?\\).*\$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun parseByteCode(parsingOptions: Int) : String {
    return AsmTraceUtils.traceVisit(classFileContext.classReader(), parsingOptions, Textifier())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = TextifiedView(classFileContext)
  }
}
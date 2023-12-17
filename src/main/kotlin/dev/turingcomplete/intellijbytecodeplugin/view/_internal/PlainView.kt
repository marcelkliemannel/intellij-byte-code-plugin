package dev.turingcomplete.intellijbytecodeplugin.view._internal

import dev.turingcomplete.intellijbytecodeplugin.bytecode.TraceUtils.traceVisit
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Textifier
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView


internal class PlainView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "Plain", METHOD_LINE_REGEX) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    // Example: "  private <init>(Ljava/lang/ClassLoader;Ljava/lang/Class;)V"
    private val METHOD_LINE_REGEX = Regex("^\\s\\s(?:[^/\\s]+\\s)*(?<name>(\\w|\\\$|_|<|>)[^\\s(]+)\\(.*?\\).*\$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun asyncParseByteCode(parsingOptions: Int, onSuccess: (String) -> Unit) {
    onSuccess(traceVisit(classFileContext.classReader(), parsingOptions, Textifier()))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = PlainView(classFileContext)
  }
}
package dev.turingcomplete.intellijbytecodeplugin.view._internal

import com.intellij.icons.AllIcons
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TraceUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Textifier
import dev.turingcomplete.intellijbytecodeplugin.view.ByteCodeParsingResultView


internal class PlainView(classFileContext: ClassFileContext)
  : ByteCodeParsingResultView(classFileContext, "Plain", AllIcons.FileTypes.JavaClass, METHOD_LINE_REGEX) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    // Example: "  private <init>(Ljava/lang/ClassLoader;Ljava/lang/Class;)V"
    private val METHOD_LINE_REGEX = Regex("^\\s\\s(?:[^/\\s]+\\s)*(?<name>(\\w|\\\$|_|<|>)[^\\s(]+)\\(.*?\\).*\$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun parseByteCode(parsingOptions: Int) : String {
    return TraceUtils.traceVisit(classFileContext.classReader(), parsingOptions, Textifier())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class MyCreator : Creator {
    override fun create(classFileContext: ClassFileContext) = PlainView(classFileContext)
  }
}
package dev.turingcomplete.intellijbytecodeplugin.asm

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.ClassReader
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Printer
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

object AsmTraceUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun traceVisit(classReader: ClassReader, parsingOptions: Int, printer: Printer): String {
    val traceWriter = StringWriter()
    val printWriter = PrintWriter(traceWriter)
    val traceClassVisitor = TraceClassVisitor(null, printer, printWriter)
    classReader.accept(traceClassVisitor, parsingOptions)
    return traceWriter.toString()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
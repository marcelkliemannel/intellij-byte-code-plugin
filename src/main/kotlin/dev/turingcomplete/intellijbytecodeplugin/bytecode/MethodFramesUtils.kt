package dev.turingcomplete.intellijbytecodeplugin.bytecode

import dev.turingcomplete.intellijbytecodeplugin._ui.DefaultClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Label
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.TypePath
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.AbstractInsnNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.MethodNode
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.analysis.Analyzer
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.analysis.BasicInterpreter
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.analysis.BasicValue
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Printer
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.Textifier
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

object MethodFramesUtils {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Exposed Methods ----------------------------------------------------- //

  fun collectFrames(method: MethodNode, owner: ClassNode): MethodFrames {
    val instructionTextifier = ExtendedTextifier()
    val frames = Analyzer(ExtendedInterpreter()).analyze(owner.name, method)
    val methodFrames = (0 until method.instructions.size()).asSequence().map { i ->
      val instruction = method.instructions[i]

      val stringWriter = StringWriter()
      val printWriter = PrintWriter(stringWriter)
      instruction.accept(TraceMethodVisitor(instructionTextifier))
      instructionTextifier.print(printWriter)
      instructionTextifier.reset()
      val textifiedInstruction = stringWriter.toString().replace("\n", "")

      val frame = frames[i]
      if (frame != null) {
        val stack = (0 until frame.stackSize).mapNotNull { j -> frame.getStack(j).type }.toTypedArray()
        val locals = (0 until frame.locals).mapNotNull { j -> frame.getLocal(j).type }.toTypedArray()
        MethodFrame(instruction, textifiedInstruction, stack, locals)
      }
      else {
        MethodFrame(instruction, textifiedInstruction, emptyArray(), emptyArray())
      }
    }.toList()

    return MethodFrames(HashMap(instructionTextifier.labelsNames()), methodFrames)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class MethodFrames internal constructor(val labelsNames: Map<Label, String>,
                                          val methodFrames: List<MethodFrame>)

  // -- Inner Type ---------------------------------------------------------- //

  class MethodFrame internal constructor(val instruction: AbstractInsnNode,
                                         val textifiedInstruction: String,
                                         val stack: Array<Type>,
                                         val locals: Array<Type>)

  // -- Inner Type ---------------------------------------------------------- //

  private class ExtendedTextifier : Textifier(DefaultClassFileContext.ASM_API) {

    init {
      tab = ""
      tab2 = "  "
      tab3 = "    "
      ltab = ""
    }

    fun labelsNames(): Map<Label, String> = labelNames ?: mapOf()

    fun reset() {
      text.clear()
    }

    override fun createTextifier(): Textifier {
      return ExtendedTextifier()
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
      // Ignore
    }

    override fun visitTryCatchAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String?, visible: Boolean): Printer? {
      // Ignore
      return null
    }

    override fun visitLocalVariable(name: String?, descriptor: String?, signature: String?, start: Label?, end: Label?, index: Int) {
      // Ignore
    }

    override fun visitLocalVariableAnnotation(typeRef: Int, typePath: TypePath?, start: Array<out Label>?, end: Array<out Label>?, index: IntArray?, descriptor: String?, visible: Boolean): Printer? {
      // Ignore
      return null
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
      // Ignore
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ExtendedInterpreter : BasicInterpreter(DefaultClassFileContext.ASM_API) {

    override fun newValue(type: Type?): BasicValue? {
      return if (type != null && (type.sort == Type.OBJECT || type.sort == Type.ARRAY)) {
        BasicValue(type)
      }
      else {
        super.newValue(type)
      }
    }
  }
}

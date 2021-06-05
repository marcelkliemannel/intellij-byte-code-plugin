package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes

@Deprecated("Not fully implemented yet.")
internal object InstructionsUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val SIMPLE_INSTRUCTIONS = arrayOf(Instruction(Opcodes.NOP, "NOP", description = "Do nothing."),
                                    Instruction(Opcodes.ACONST_NULL, "ACONST_NULL", stackAfter = listOf("null"), description = "Push <code>null</code>."),
                                    Instruction(Opcodes.ICONST_M1, "ICONST_M1", stackAfter = listOf("-1"), description = "Push <code>int</code> constant <code>-1</code>."),
                                    Instruction(Opcodes.ICONST_0, "ICONST_0", stackAfter = listOf("0"), description = "Push <code>int</code> constant <code>0</code>."),
                                    Instruction(Opcodes.ICONST_1, "ICONST_1", stackAfter = listOf("1"), description = "Push <code>int</code> constant <code>1</code>."),
                                    Instruction(Opcodes.ICONST_2, "ICONST_2", stackAfter = listOf("2"), description = "Push <code>int</code> constant <code>2</code>."),
                                    Instruction(Opcodes.ICONST_3, "ICONST_3", stackAfter = listOf("3"), description = "Push <code>int</code> constant <code>3</code>."),
                                    Instruction(Opcodes.ICONST_4, "ICONST_4", stackAfter = listOf("4"), description = "Push <code>int</code> constant <code>4</code>."),
                                    Instruction(Opcodes.ICONST_5, "ICONST_5", stackAfter = listOf("5"), description = "Push <code>int</code> constant <code>5</code>."),
                                    Instruction(Opcodes.LCONST_0, "LCONST_0", stackAfter = listOf("0L"), description = "Push <code>long</code> constant <code>0</code>."),
                                    Instruction(Opcodes.LCONST_1, "LCONST_1", stackAfter = listOf("1L"), description = "Push <code>long</code> constant <code>1</code>."),
                                    Instruction(Opcodes.FCONST_0, "FCONST_0", stackAfter = listOf("0.0f"), description = "Push <code>float</code> constant <code>0.0f</code>."),
                                    Instruction(Opcodes.FCONST_1, "FCONST_1", stackAfter = listOf("1.0f"), description = "Push <code>float</code> constant <code>1.0f</code>."),
                                    Instruction(Opcodes.FCONST_2, "FCONST_2", stackAfter = listOf("2.0f"), description = "Push <code>float</code> constant <code>2.0f</code>."),
                                    Instruction(Opcodes.DCONST_0, "DCONST_0", stackAfter = listOf("0.0"), description = "Push <code>float</code> constant <code>0.0f</code>."),
                                    Instruction(Opcodes.DCONST_1, "DCONST_1", stackAfter = listOf("1.0"), description = "Push <code>float</code> constant <code>1.0f</code>."),
                                    Instruction(Opcodes.LALOAD, "LALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>long</code> from array."),
                                    Instruction(Opcodes.FALOAD, "FALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>float</code> from array."),
                                    Instruction(Opcodes.DALOAD, "DALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>double</code> from array."),
                                    Instruction(Opcodes.AALOAD, "AALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>reference</code> from array."),
                                    Instruction(Opcodes.BALOAD, "BALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>byte</code> from array."),
                                    Instruction(Opcodes.CALOAD, "CALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>char</code> from array."),
                                    Instruction(Opcodes.SALOAD, "SALOAD", stackBefore = listOf("Array reference", "Index"), stackAfter = listOf("Value"), description = "Load <code>short</code> from array."),
                                    Instruction(Opcodes.LASTORE, "LASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>long</code> to array."),
                                    Instruction(Opcodes.FASTORE, "FASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>float</code> to array."),
                                    Instruction(Opcodes.DASTORE, "DASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>double</code> to array."),
                                    Instruction(Opcodes.AASTORE, "AASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>reference</code> to array."),
                                    Instruction(Opcodes.BASTORE, "BASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>byte</code> to array."),
                                    Instruction(Opcodes.CASTORE, "CASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>char</code> to array."),
                                    Instruction(Opcodes.SASTORE, "SASTORE", stackBefore = listOf("Array reference", "Index", "Value"), description = "Store <code>short</code> to array."),
                                    Instruction(Opcodes.POP, "POP", stackBefore = listOf("Value"), description = "Pop the top operand stack value."),
                                    Instruction(Opcodes.POP2, "POP2", stackBefore = listOf("Value 2", "Value 1"), description = "Pop the top one or two operand stack values."),
                                    Instruction(Opcodes.DUP, "DUP"),
                                    Instruction(Opcodes.DUP_X1, "DUP_X1"),
                                    Instruction(Opcodes.DUP_X2, "DUP_X2"),
                                    Instruction(Opcodes.DUP2, "DUP2"),
                                    Instruction(Opcodes.DUP2_X1, "DUP2_X1"),
                                    Instruction(Opcodes.DUP2_X2, "DUP2_X2"),
                                    Instruction(Opcodes.SWAP, "SWAP"),
                                    Instruction(Opcodes.IADD, "IADD"),
                                    Instruction(Opcodes.LADD, "LADD"),
                                    Instruction(Opcodes.FADD, "FADD"),
                                    Instruction(Opcodes.DADD, "DADD"),
                                    Instruction(Opcodes.ISUB, "ISUB "),
                                    Instruction(Opcodes.LSUB, "LSUB "),
                                    Instruction(Opcodes.FSUB, "FSUB"),
                                    Instruction(Opcodes.DSUB, "DSUB"),
                                    Instruction(Opcodes.IMUL, "IMUL"),
                                    Instruction(Opcodes.LMUL, "LMUL"),
                                    Instruction(Opcodes.FMUL, "FMUL"),
                                    Instruction(Opcodes.DMUL, "DMUL"),
                                    Instruction(Opcodes.IDIV, "IDIV"),
                                    Instruction(Opcodes.LDIV, "LDIV"),
                                    Instruction(Opcodes.FDIV, "FDIV"),
                                    Instruction(Opcodes.DDIV, "DDIV"),
                                    Instruction(Opcodes.IREM, "IREM"),
                                    Instruction(Opcodes.LREM, "LREM"),
                                    Instruction(Opcodes.FREM, "FREM"),
                                    Instruction(Opcodes.DREM, "DREM"),
                                    Instruction(Opcodes.INEG, "INEG"),
                                    Instruction(Opcodes.LNEG, "LNEG"),
                                    Instruction(Opcodes.FNEG, "FNEG"),
                                    Instruction(Opcodes.DNEG, "DNEG"),
                                    Instruction(Opcodes.ISHL, "ISHL"),
                                    Instruction(Opcodes.LSHL, "LSHL"),
                                    Instruction(Opcodes.ISHR, "ISHR"),
                                    Instruction(Opcodes.LSHR, "LSHR"),
                                    Instruction(Opcodes.IUSHR, "IUSHR"),
                                    Instruction(Opcodes.LUSHR, "LUSHR"),
                                    Instruction(Opcodes.IAND, "IAND"),
                                    Instruction(Opcodes.LAND, "LAND"),
                                    Instruction(Opcodes.IOR, "IOR"),
                                    Instruction(Opcodes.LOR, "LOR"),
                                    Instruction(Opcodes.IXOR, "IXOR"),
                                    Instruction(Opcodes.LXOR, "LXOR"),
                                    Instruction(Opcodes.I2L, "I2L"),
                                    Instruction(Opcodes.I2F, "I2F"),
                                    Instruction(Opcodes.I2D, "I2D"),
                                    Instruction(Opcodes.L2I, "L2I"),
                                    Instruction(Opcodes.L2F, "L2F"),
                                    Instruction(Opcodes.L2D, "L2D"),
                                    Instruction(Opcodes.F2I, "F2I"),
                                    Instruction(Opcodes.F2L, "F2L"),
                                    Instruction(Opcodes.F2D, "F2D"),
                                    Instruction(Opcodes.D2I, "D2I"),
                                    Instruction(Opcodes.D2L, "D2L"),
                                    Instruction(Opcodes.D2F, "D2F"),
                                    Instruction(Opcodes.I2B, "I2B"),
                                    Instruction(Opcodes.I2C, "I2C"),
                                    Instruction(Opcodes.I2S, "I2S"),
                                    Instruction(Opcodes.LCMP, "LCMP"),
                                    Instruction(Opcodes.FCMPL, "FCMPL"),
                                    Instruction(Opcodes.FCMPG, "FCMPG"),
                                    Instruction(Opcodes.DCMPL, "DCMPL"),
                                    Instruction(Opcodes.DCMPG, "DCMPG"),
                                    Instruction(Opcodes.IASTORE, "IASTORE"),
                                    Instruction(Opcodes.IALOAD, "IALOAD"),
                                    Instruction(Opcodes.IRETURN, "IRETURN "),
                                    Instruction(Opcodes.MONITORENTER, "MONITORENTER"),
                                    Instruction(Opcodes.MONITOREXIT, "MONITOREXIT "),
                                    Instruction(Opcodes.ARRAYLENGTH, "ARRAYLENGTH"),
                                    Instruction(Opcodes.ATHROW, "ATHROW"),
                                    Instruction(Opcodes.LRETURN, "LRETURN"),
                                    Instruction(Opcodes.FRETURN, "FRETURN"),
                                    Instruction(Opcodes.DRETURN, "DRETURN"),
                                    Instruction(Opcodes.ARETURN, "ARETURN"),
                                    Instruction(Opcodes.RETURN, "RETURN"))

  val JUMP_INSTRUCTIONS = arrayOf(Instruction(Opcodes.IFEQ, "IFEQ"),
                                  Instruction(Opcodes.IFNE, "IFNE"),
                                  Instruction(Opcodes.IFLT, "IFLT"),
                                  Instruction(Opcodes.IFGE, "IFGE"),
                                  Instruction(Opcodes.IFGT, "IFGT"),
                                  Instruction(Opcodes.IFLE, "IFLE"),
                                  Instruction(Opcodes.IF_ICMPEQ, "IF_ICMPEQ"),
                                  Instruction(Opcodes.IF_ICMPNE, "IF_ICMPNE"),
                                  Instruction(Opcodes.IF_ICMPLT, "IF_ICMPLT"),
                                  Instruction(Opcodes.IF_ICMPGE, "IF_ICMPGE"),
                                  Instruction(Opcodes.IF_ICMPGT, "IF_ICMPGT"),
                                  Instruction(Opcodes.IF_ICMPLE, "IF_ICMPLE"),
                                  Instruction(Opcodes.IF_ACMPEQ, "IF_ACMPEQ"),
                                  Instruction(Opcodes.IF_ACMPNE, "IF_ACMPNE"),
                                  Instruction(Opcodes.GOTO, "GOTO"),
                                  Instruction(Opcodes.JSR, "JSR"),
                                  Instruction(Opcodes.IFNULL, "IFNULL"),
                                  Instruction(Opcodes.IFNONNULL, "IFNONNULL"))

  val VARIABLE_INSTRUCTIONS = arrayOf(Instruction(Opcodes.ILOAD, "ILOAD"),
                                      Instruction(Opcodes.LLOAD, "LLOAD"),
                                      Instruction(Opcodes.FLOAD, "FLOAD"),
                                      Instruction(Opcodes.DLOAD, "DLOAD"),
                                      Instruction(Opcodes.ALOAD, "ALOAD"),
                                      Instruction(Opcodes.ISTORE, "ISTORE"),
                                      Instruction(Opcodes.LSTORE, "LSTORE"),
                                      Instruction(Opcodes.FSTORE, "FSTORE"),
                                      Instruction(Opcodes.DSTORE, "DSTORE"),
                                      Instruction(Opcodes.ASTORE, "ASTORE"),
                                      Instruction(Opcodes.RET, "RET"))

  val INTEGER_INSTRUCTIONS = arrayOf(Instruction(Opcodes.BIPUSH, "BIPUSH"),
                                     Instruction(Opcodes.SIPUSH, "SIPUSH"),
                                     Instruction(Opcodes.NEWARRAY, "NEWARRAY"))

  val TYPE_INSTRUCTIONS = arrayOf(Instruction(Opcodes.NEW, "NEW"),
                                  Instruction(Opcodes.ANEWARRAY, "ANEWARRAY"),
                                  Instruction(Opcodes.CHECKCAST, "CHECKCAST"),
                                  Instruction(Opcodes.INSTANCEOF, "INSTANCEOF"))

  val METHOD_INSTRUCTIONS = arrayOf(Instruction(Opcodes.INVOKEVIRTUAL, "INVOKEVIRTUAL"),
                                    Instruction(Opcodes.INVOKESPECIAL, "INVOKESPECIAL"),
                                    Instruction(Opcodes.INVOKESTATIC, "INVOKESTATIC"),
                                    Instruction(Opcodes.INVOKEINTERFACE, "INVOKEINTERFACE"))

  val FIELD_INSTRUCTIONS = arrayOf(Instruction(Opcodes.GETSTATIC, "GETSTATIC"),
                                   Instruction(Opcodes.PUTSTATIC, "PUTSTATIC"),
                                   Instruction(Opcodes.GETFIELD, "GETFIELD"),
                                   Instruction(Opcodes.PUTFIELD, "PUTFIELD"))

  val LDC_INSTRUCTION = Instruction(Opcodes.LDC, "LDC")

  val IINC_INSTRUCTION = Instruction(Opcodes.IINC, "IINC")

  val TABLE_SWITCH_INSTRUCTION = Instruction(Opcodes.TABLESWITCH, "TABLESWITCH")

  val LOOK_UP_SWITCH__INSTRUCTION = Instruction(Opcodes.LOOKUPSWITCH, "LOOKUPSWITCH")

  val INVOKE_DYNAMIC_INSTRUCTION = Instruction(Opcodes.INVOKEDYNAMIC, "INVOKEDYNAMIC")

  val MULTI_NEW_ARRAY_INSTRUCTION = Instruction(Opcodes.MULTIANEWARRAY, "MULTIANEWARRAY", listOf("Index byte 1", "Index byte 2"), listOf())

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Instruction(val opcode: Int,
                    val name: String,
                    val arguments: List<String> = listOf(),
                    val stackBefore: List<String> = listOf(),
                    val stackAfter: List<String> = listOf(),
                    val description: String? = null)
}
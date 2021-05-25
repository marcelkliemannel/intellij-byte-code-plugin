package dev.turingcomplete.intellijbytecodeplugin.asm

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes

object AsmTextifierUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val instructions = arrayOf(Pair(Opcodes.NOP, "NOP"),
                             Pair(Opcodes.ACONST_NULL, "ACONST_NULL"),
                             Pair(Opcodes.ICONST_M1, "ICONST_M1"),
                             Pair(Opcodes.ICONST_0, "ICONST_0"),
                             Pair(Opcodes.ICONST_1, "ICONST_1"),
                             Pair(Opcodes.ICONST_2, "ICONST_2"),
                             Pair(Opcodes.ICONST_3, "ICONST_3"),
                             Pair(Opcodes.ICONST_4, "ICONST_4"),
                             Pair(Opcodes.ICONST_5, "ICONST_5"),
                             Pair(Opcodes.LCONST_0, "LCONST_0"),
                             Pair(Opcodes.LCONST_1, "LCONST_1"),
                             Pair(Opcodes.FCONST_0, "FCONST_0"),
                             Pair(Opcodes.FCONST_1, "FCONST_1"),
                             Pair(Opcodes.FCONST_2, "FCONST_2"),
                             Pair(Opcodes.DCONST_0, "DCONST_0"),
                             Pair(Opcodes.DCONST_1, "DCONST_1"),
                             Pair(Opcodes.LALOAD, "LALOAD"),
                             Pair(Opcodes.FALOAD, "FALOAD"),
                             Pair(Opcodes.DALOAD, "DALOAD"),
                             Pair(Opcodes.AALOAD, "AALOAD"),
                             Pair(Opcodes.BALOAD, "BALOAD"),
                             Pair(Opcodes.CALOAD, "CALOAD"),
                             Pair(Opcodes.SALOAD, "SALOAD"),
                             Pair(Opcodes.LASTORE, "LASTORE"),
                             Pair(Opcodes.FASTORE, "FASTORE"),
                             Pair(Opcodes.DASTORE, "DASTORE"),
                             Pair(Opcodes.AASTORE, "AASTORE"),
                             Pair(Opcodes.BASTORE, "BASTORE"),
                             Pair(Opcodes.CASTORE, "CASTORE"),
                             Pair(Opcodes.SASTORE, "SASTORE"),
                             Pair(Opcodes.POP, "POP"),
                             Pair(Opcodes.POP2, "POP2"),
                             Pair(Opcodes.DUP, "DUP"),
                             Pair(Opcodes.DUP_X1, "DUP_X1"),
                             Pair(Opcodes.DUP_X2, "DUP_X2"),
                             Pair(Opcodes.DUP2, "DUP2"),
                             Pair(Opcodes.DUP2_X1, "DUP2_X1"),
                             Pair(Opcodes.DUP2_X2, "DUP2_X2"),
                             Pair(Opcodes.SWAP, "SWAP"),
                             Pair(Opcodes.IADD, "IADD"),
                             Pair(Opcodes.LADD, "LADD"),
                             Pair(Opcodes.FADD, "FADD"),
                             Pair(Opcodes.DADD, "DADD"),
                             Pair(Opcodes.ISUB, "ISUB "),
                             Pair(Opcodes.LSUB, "LSUB "),
                             Pair(Opcodes.FSUB, "FSUB"),
                             Pair(Opcodes.DSUB, "DSUB"),
                             Pair(Opcodes.IMUL, "IMUL"),
                             Pair(Opcodes.LMUL, "LMUL"),
                             Pair(Opcodes.FMUL, "FMUL"),
                             Pair(Opcodes.DMUL, "DMUL"),
                             Pair(Opcodes.IDIV, "IDIV"),
                             Pair(Opcodes.LDIV, "LDIV"),
                             Pair(Opcodes.FDIV, "FDIV"),
                             Pair(Opcodes.DDIV, "DDIV"),
                             Pair(Opcodes.IREM, "IREM"),
                             Pair(Opcodes.LREM, "LREM"),
                             Pair(Opcodes.FREM, "FREM"),
                             Pair(Opcodes.DREM, "DREM"),
                             Pair(Opcodes.INEG, "INEG"),
                             Pair(Opcodes.LNEG, "LNEG"),
                             Pair(Opcodes.FNEG, "FNEG"),
                             Pair(Opcodes.DNEG, "DNEG"),
                             Pair(Opcodes.ISHL, "ISHL"),
                             Pair(Opcodes.LSHL, "LSHL"),
                             Pair(Opcodes.ISHR, "ISHR"),
                             Pair(Opcodes.LSHR, "LSHR"),
                             Pair(Opcodes.IUSHR, "IUSHR"),
                             Pair(Opcodes.LUSHR, "LUSHR"),
                             Pair(Opcodes.IAND, "IAND"),
                             Pair(Opcodes.LAND, "LAND"),
                             Pair(Opcodes.IOR, "IOR"),
                             Pair(Opcodes.LOR, "LOR"),
                             Pair(Opcodes.IXOR, "IXOR"),
                             Pair(Opcodes.LXOR, "LXOR"),
                             Pair(Opcodes.I2L, "I2L"),
                             Pair(Opcodes.I2F, "I2F"),
                             Pair(Opcodes.I2D, "I2D"),
                             Pair(Opcodes.L2I, "L2I"),
                             Pair(Opcodes.L2F, "L2F"),
                             Pair(Opcodes.L2D, "L2D"),
                             Pair(Opcodes.F2I, "F2I"),
                             Pair(Opcodes.F2L, "F2L"),
                             Pair(Opcodes.F2D, "F2D"),
                             Pair(Opcodes.D2I, "D2I"),
                             Pair(Opcodes.D2L, "D2L"),
                             Pair(Opcodes.D2F, "D2F"),
                             Pair(Opcodes.I2B, "I2B"),
                             Pair(Opcodes.I2C, "I2C"),
                             Pair(Opcodes.I2S, "I2S"),
                             Pair(Opcodes.LCMP, "LCMP"),
                             Pair(Opcodes.FCMPL, "FCMPL"),
                             Pair(Opcodes.FCMPG, "FCMPG"),
                             Pair(Opcodes.DCMPL, "DCMPL"),
                             Pair(Opcodes.DCMPG, "DCMPG"),
                             Pair(Opcodes.IASTORE, "IASTORE"),
                             Pair(Opcodes.IALOAD, "IALOAD"),
                             Pair(Opcodes.IRETURN, "IRETURN "),
                             Pair(Opcodes.MONITORENTER, "MONITORENTER"),
                             Pair(Opcodes.MONITOREXIT, "MONITOREXIT "),
                             Pair(Opcodes.ARRAYLENGTH, "ARRAYLENGTH"),
                             Pair(Opcodes.ATHROW, "ATHROW"),
                             Pair(Opcodes.LRETURN, "LRETURN"),
                             Pair(Opcodes.FRETURN, "FRETURN"),
                             Pair(Opcodes.DRETURN, "DRETURN"),
                             Pair(Opcodes.ARETURN, "ARETURN"),
                             Pair(Opcodes.RETURN, "RETURN"))

  val jumpInstructions = arrayOf(Pair(Opcodes.IFEQ, "IFEQ"),
                                 Pair(Opcodes.IFNE, "IFNE"),
                                 Pair(Opcodes.IFLT, "IFLT"),
                                 Pair(Opcodes.IFGE, "IFGE"),
                                 Pair(Opcodes.IFGT, "IFGT"),
                                 Pair(Opcodes.IFLE, "IFLE"),
                                 Pair(Opcodes.IF_ICMPEQ, "IF_ICMPEQ"),
                                 Pair(Opcodes.IF_ICMPNE, "IF_ICMPNE"),
                                 Pair(Opcodes.IF_ICMPLT, "IF_ICMPLT"),
                                 Pair(Opcodes.IF_ICMPGE, "IF_ICMPGE"),
                                 Pair(Opcodes.IF_ICMPGT, "IF_ICMPGT"),
                                 Pair(Opcodes.IF_ICMPLE, "IF_ICMPLE"),
                                 Pair(Opcodes.IF_ACMPEQ, "IF_ACMPEQ"),
                                 Pair(Opcodes.IF_ACMPNE, "IF_ACMPNE"),
                                 Pair(Opcodes.GOTO, "GOTO"),
                                 Pair(Opcodes.JSR, "JSR"),
                                 Pair(Opcodes.IFNULL, "IFNULL"),
                                 Pair(Opcodes.IFNONNULL, "IFNONNULL"))

  val variableInstructions = arrayOf(Pair(Opcodes.ILOAD, "ILOAD"),
                                     Pair(Opcodes.LLOAD, "LLOAD"),
                                     Pair(Opcodes.FLOAD, "FLOAD"),
                                     Pair(Opcodes.DLOAD, "DLOAD"),
                                     Pair(Opcodes.ALOAD, "ALOAD"),
                                     Pair(Opcodes.ISTORE, "ISTORE"),
                                     Pair(Opcodes.LSTORE, "LSTORE"),
                                     Pair(Opcodes.FSTORE, "FSTORE"),
                                     Pair(Opcodes.DSTORE, "DSTORE"),
                                     Pair(Opcodes.ASTORE, "ASTORE"),
                                     Pair(Opcodes.RET, "RET"))

  val integerInstructions = arrayOf(Pair(Opcodes.BIPUSH, "BIPUSH"),
                                    Pair(Opcodes.SIPUSH, "SIPUSH"),
                                    Pair(Opcodes.NEWARRAY, "NEWARRAY"))

  val typeInstructions = arrayOf(Pair(Opcodes.NEW, "NEW"),
                                 Pair(Opcodes.ANEWARRAY, "ANEWARRAY"),
                                 Pair(Opcodes.CHECKCAST, "CHECKCAST"),
                                 Pair(Opcodes.INSTANCEOF, "INSTANCEOF"))

  val methodInstructions = arrayOf(Pair(Opcodes.INVOKEVIRTUAL, "INVOKEVIRTUAL"),
                                   Pair(Opcodes.INVOKESPECIAL, "INVOKESPECIAL"),
                                   Pair(Opcodes.INVOKESTATIC, "INVOKESTATIC"),
                                   Pair(Opcodes.INVOKEINTERFACE, "INVOKEINTERFACE"))

  val fieldInstructions = arrayOf(Pair(Opcodes.GETSTATIC, "GETSTATIC"),
                                  Pair(Opcodes.PUTSTATIC, "PUTSTATIC"),
                                  Pair(Opcodes.GETFIELD, "GETFIELD"),
                                  Pair(Opcodes.PUTFIELD, "PUTFIELD"))

  val ldcInstruction = Pair(Opcodes.LDC, "LDC")

  val iIncInstruction = Pair(Opcodes.IINC, "IINC")

  val tableSwitchInstruction = Pair(Opcodes.TABLESWITCH, "TABLESWITCH")

  val lookUpSwitchInstruction = Pair(Opcodes.LOOKUPSWITCH, "LOOKUPSWITCH")

  val invokeDynamicInstruction = Pair(Opcodes.INVOKEDYNAMIC, "INVOKEDYNAMIC")

  val multiANewArrayInstruction = Pair(Opcodes.MULTIANEWARRAY, "MULTIANEWARRAY")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
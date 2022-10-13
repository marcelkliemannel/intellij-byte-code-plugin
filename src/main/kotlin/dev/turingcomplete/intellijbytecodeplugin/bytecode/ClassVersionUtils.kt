package dev.turingcomplete.intellijbytecodeplugin.bytecode

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes

object ClassVersionUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val CLASS_VERSIONS = arrayOf(ClassVersion(Opcodes.V20.toByte(), "Java SE 20"),
                               ClassVersion(Opcodes.V19.toByte(), "Java SE 19"),
                               ClassVersion(Opcodes.V18.toByte(), "Java SE 18"),
                               ClassVersion(Opcodes.V17.toByte(), "Java SE 17"),
                               ClassVersion(Opcodes.V16.toByte(), "Java SE 16"),
                               ClassVersion(Opcodes.V15.toByte(), "Java SE 15"),
                               ClassVersion(Opcodes.V14.toByte(), "Java SE 14"),
                               ClassVersion(Opcodes.V13.toByte(), "Java SE 13"),
                               ClassVersion(Opcodes.V12.toByte(), "Java SE 12"),
                               ClassVersion(Opcodes.V11.toByte(), "Java SE 11"),
                               ClassVersion(Opcodes.V10.toByte(), "Java SE 10"),
                               ClassVersion(Opcodes.V9.toByte(), "Java SE 9"),
                               ClassVersion(Opcodes.V1_8.toByte(), "Java SE 8"),
                               ClassVersion(Opcodes.V1_7.toByte(), "Java SE 7"),
                               ClassVersion(Opcodes.V1_6.toByte(), "Java SE 6.0"),
                               ClassVersion(Opcodes.V1_5.toByte(), "Java SE 5.0"),
                               ClassVersion(Opcodes.V1_4.toByte(), "JDK 1.4"),
                               ClassVersion(Opcodes.V1_3.toByte(), "JDK 1.3"),
                               ClassVersion(Opcodes.V1_2.toByte(), "JDK 1.2"),
                               ClassVersion(Opcodes.V1_1.toByte(), "JDK 1.1"))

  val MAJOR_TO_CLASS_VERSION: Map<Byte, ClassVersion> = CLASS_VERSIONS.associateBy { it.major }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun toClassVersion(asmClassVersion: Int): ClassVersion? {
    val major = (asmClassVersion and 0xFF).toByte()
    val minor = (asmClassVersion shr 8 and 0xFF).toByte()
    return if (minor == 0.toByte()) MAJOR_TO_CLASS_VERSION[major] else null
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  data class ClassVersion(val major: Byte, val specification: String) {

    override fun toString() = "$major ($specification)"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
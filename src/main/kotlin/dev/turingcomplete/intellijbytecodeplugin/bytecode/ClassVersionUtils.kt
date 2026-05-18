package dev.turingcomplete.intellijbytecodeplugin.bytecode

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes

object ClassVersionUtils {
  // -- Properties ---------------------------------------------------------- //

  val CLASS_VERSIONS =
    arrayOf(
      ClassVersion(Opcodes.V27, "Java SE 27"),
      ClassVersion(Opcodes.V26, "Java SE 26"),
      ClassVersion(Opcodes.V25, "Java SE 25"),
      ClassVersion(Opcodes.V24, "Java SE 24"),
      ClassVersion(Opcodes.V23, "Java SE 23"),
      ClassVersion(Opcodes.V22, "Java SE 22"),
      ClassVersion(Opcodes.V21, "Java SE 21"),
      ClassVersion(Opcodes.V20, "Java SE 20"),
      ClassVersion(Opcodes.V19, "Java SE 19"),
      ClassVersion(Opcodes.V18, "Java SE 18"),
      ClassVersion(Opcodes.V17, "Java SE 17"),
      ClassVersion(Opcodes.V16, "Java SE 16"),
      ClassVersion(Opcodes.V15, "Java SE 15"),
      ClassVersion(Opcodes.V14, "Java SE 14"),
      ClassVersion(Opcodes.V13, "Java SE 13"),
      ClassVersion(Opcodes.V12, "Java SE 12"),
      ClassVersion(Opcodes.V11, "Java SE 11"),
      ClassVersion(Opcodes.V10, "Java SE 10"),
      ClassVersion(Opcodes.V9, "Java SE 9"),
      ClassVersion(Opcodes.V1_8, "Java SE 8"),
      ClassVersion(Opcodes.V1_7, "Java SE 7"),
      ClassVersion(Opcodes.V1_6, "Java SE 6.0"),
      ClassVersion(Opcodes.V1_5, "Java SE 5.0"),
      ClassVersion(Opcodes.V1_4, "JDK 1.4"),
      ClassVersion(Opcodes.V1_3, "JDK 1.3"),
      ClassVersion(Opcodes.V1_2, "JDK 1.2"),
      ClassVersion(Opcodes.V1_1 and 0xFFFF, "JDK 1.1"),
    )

  val MAJOR_TO_CLASS_VERSION: Map<Int, ClassVersion> = CLASS_VERSIONS.associateBy { it.major }

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun toClassVersion(asmClassVersion: Int): ClassVersion? {
    val (major, minor) = parseMajorMinor(asmClassVersion)
    return if (minor == 0 || major == Opcodes.V1_1 || isPreviewClassVersion(asmClassVersion)) {
      MAJOR_TO_CLASS_VERSION[major]
    } else null
  }

  fun toMajorMinorString(asmClassVersion: Int): String {
    val (major, minor) = parseMajorMinor(asmClassVersion)
    return major.toString() + (if (minor != 0) ".$minor" else "")
  }

  fun isPreviewClassVersion(asmClassVersion: Int): Boolean {
    val (_, minor) = parseMajorMinor(asmClassVersion)
    return minor == (Opcodes.V_PREVIEW ushr 16)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun parseMajorMinor(asmClassVersion: Int): Pair<Int, Int> {
    val major = asmClassVersion and 0xFFFF
    val minor = asmClassVersion ushr 16
    return Pair(major, minor)
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class ClassVersion(val major: Int, val specification: String) {

    override fun toString() = "$major ($specification)"
  }
}

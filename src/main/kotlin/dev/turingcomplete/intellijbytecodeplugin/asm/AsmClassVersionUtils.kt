package dev.turingcomplete.intellijbytecodeplugin.asm

object AsmClassVersionUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val CLASS_VERSIONS = arrayOf(ClassVersion(61, "Java SE 17"),
                               ClassVersion(60, "Java SE 16"),
                               ClassVersion(59, "Java SE 15"),
                               ClassVersion(58, "Java SE 14"),
                               ClassVersion(57, "Java SE 13"),
                               ClassVersion(56, "Java SE 12"),
                               ClassVersion(55, "Java SE 11"),
                               ClassVersion(54, "Java SE 10"),
                               ClassVersion(53, "Java SE 9"),
                               ClassVersion(52, "Java SE 8"),
                               ClassVersion(51, "Java SE 7"),
                               ClassVersion(50, "Java SE 6.0"),
                               ClassVersion(49, "Java SE 5.0"),
                               ClassVersion(48, "JDK 1.4"),
                               ClassVersion(47, "JDK 1.3"),
                               ClassVersion(46, "JDK 1.2"),
                               ClassVersion(45, "JDK 1.1"))

  val MAJOR_TO_CLASS_VERSION: Map<Byte, ClassVersion> = CLASS_VERSIONS.map { it.major to it }.toMap()

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
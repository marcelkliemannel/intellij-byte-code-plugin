package dev.turingcomplete.intellijbytecodeplugin.bytecode

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import java.lang.reflect.Modifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ClassVersionUtilsTest {
  // -- Exported Methods
  // -------------------------------------------------------------------------------------------- //

  @Test
  fun `Given ASM class version opcodes, Then all are mapped to a class version`() {
    val mappedClassVersions = ClassVersionUtils.CLASS_VERSIONS.map { it.major }.toSet()

    val unmappedOpcodes =
      Opcodes::class
        .java
        .fields
        .filter { it.name.matches(CLASS_VERSION_OPCODE_NAME_PATTERN) }
        .filter { Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
        .map { it.name to (it.getInt(null) and 0xFFFF) }
        .filterNot { (_, major) -> major in mappedClassVersions }
        .map { (name, major) -> "$name ($major)" }

    assertThat(unmappedOpcodes).isEmpty()
  }

  @Test
  fun `Given preview class version, Then major and minor version are shown`() {
    val previewVersion = Opcodes.V26 or Opcodes.V_PREVIEW

    assertThat(ClassVersionUtils.toMajorMinorString(previewVersion)).isEqualTo("70.65535")
    assertThat(ClassVersionUtils.toClassVersion(previewVersion)?.specification)
      .isEqualTo("Java SE 26")
    assertThat(ClassVersionUtils.isPreviewClassVersion(previewVersion)).isTrue()
  }

  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {

    private val CLASS_VERSION_OPCODE_NAME_PATTERN = Regex("""V(1_\d+|\d+)""")
  }
}

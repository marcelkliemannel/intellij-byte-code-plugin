package dev.turingcomplete.intellijbytecodeplugin._internal.constantpool

import dev.turingcomplete.intellijbytecodeplugin.ClassFileConsumerTestCase
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPool
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.junit.runners.Parameterized::class)
class ConstantPoolTest(
  @Suppress("UNUSED_PARAMETER") testName: String,
  classFilePaths: List<String>,
) : ClassFileConsumerTestCase(classFilePaths) {
  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<Any>> = testData()
  }

  // -- Properties
  // -------------------------------------------------------------------------------------------------- //
  // -- Initialization
  // ----------------------------------------------------------------------------------------------
  // //
  // -- Exposed Methods
  // ---------------------------------------------------------------------------------------------
  // //
  // -- Private Methods
  // ---------------------------------------------------------------------------------------------
  // //

  @Test
  fun testCreationOfConstantPool() {
    // We don't have an expected result here to compare with. This test should only
    // ensure, that there are no exceptions.
    consumeClassFiles { classFileAsVirtualFile ->
      ConstantPool.create(ClassFile(classFileAsVirtualFile))
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //
}

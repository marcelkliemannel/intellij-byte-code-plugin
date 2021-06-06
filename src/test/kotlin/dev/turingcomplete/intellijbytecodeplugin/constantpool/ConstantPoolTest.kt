package dev.turingcomplete.intellijbytecodeplugin.constantpool

import dev.turingcomplete.intellijbytecodeplugin.ClassFileConsumerTestCase
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPool
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.junit.runners.Parameterized::class)
class ConstantPoolTest(testName: String, classFilePath: String) : ClassFileConsumerTestCase(classFilePath) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<String>> = testData()
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  @Test
  fun testCreationOfConstantPool() {
    ConstantPool.create(virtualFile)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
package dev.turingcomplete.intellijbytecodeplugin.constantpool

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import dev.turingcomplete.intellijbytecodeplugin.TestUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool.ConstantPool
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.junit.runners.Parameterized::class)
class ConstantPoolTest(testName: String, private val classFilePath: String) : LightJavaCodeInsightFixtureTestCase() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<String>> = TestUtils.data()
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var virtualFile: VirtualFile

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun setUp() {
    super.setUp()

    WriteAction.runAndWait<Throwable> {
      FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, "jmod")
    }

    val virtualFile0 = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
    Assert.assertNotNull("File $classFilePath not found", virtualFile0)
    virtualFile = virtualFile0 as VirtualFile
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  @Test
  fun testCreationOfConstantPool() {
    ConstantPool.create(virtualFile)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
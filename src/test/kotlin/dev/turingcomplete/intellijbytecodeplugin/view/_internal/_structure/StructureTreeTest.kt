package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatform4TestCase
import dev.turingcomplete.intellijbytecodeplugin._ui.DefaultClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmMethodUtils
import dev.turingcomplete.intellijbytecodeplugin.asm.AsmTypeUtils
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode
import junit.framework.AssertionFailedError
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import javax.swing.tree.TreeNode

@RunWith(org.junit.runners.Parameterized::class)
class StructureTreeTest(testName: String, private val classFilePath: String) : LightPlatform4TestCase() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<String>> {
      val testParameters = mutableListOf<Array<String>>()

      // Test parsing of Kotlin classes
      System.getProperty("java.class.path").split(System.getProperty("path.separator"))
              .asSequence()
              .map { Path.of(it) }
              .filter { it.fileName.toString().startsWith("kotlin-stdlib") }
              .forEach { kotlinStdLib -> testParameters.addAll(readArchiveEntriesPaths(kotlinStdLib.toFile())) }
      Assert.assertTrue(testParameters.size > 100)

      // Test parsing of java.base classes
      val javaBaseJmodPath = Path.of(System.getProperty("java.home")).resolve(Path.of("jmods", "java.base.jmod"))
      testParameters.addAll(readArchiveEntriesPaths(javaBaseJmodPath.toFile()))
      Assert.assertTrue(testParameters.size > 200)

      return testParameters
    }

    private fun readArchiveEntriesPaths(archiveFile: File) : List<Array<String>> {
      println(archiveFile)
      val entriesPaths = mutableListOf<Array<String>>()

      ZipFile(archiveFile).use { zipFile ->
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
          val zipEntry = entries.nextElement()
          if (zipEntry.name.endsWith(".class")) {
            entriesPaths.add(arrayOf(zipEntry.name, "jar://$archiveFile!/${zipEntry.name}"))
          }
        }
      }

      return entriesPaths
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val structureTreeContextPermutations = mutableListOf<StructureTreeContext>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun setUp() {
    super.setUp()
    WriteAction.runAndWait<Throwable> {
      FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, "jmod")
    }

    val defaultStructureTreeContext = StructureTreeContext(project) {}
    structureTreeContextPermutations.add(defaultStructureTreeContext)
    AsmTypeUtils.TypeNameRenderMode.values()
            .filter { defaultStructureTreeContext.typeNameRenderMode != it }
            .forEach {
              structureTreeContextPermutations.add(StructureTreeContext(project) {}.apply { typeNameRenderMode = it })
            }
    AsmMethodUtils.MethodDescriptorRenderMode.values()
            .filter { defaultStructureTreeContext.methodDescriptorRenderMode != it }
            .forEach {
              structureTreeContextPermutations.add(StructureTreeContext(project) {}.apply { methodDescriptorRenderMode = it })
            }
  }

  @Test
  fun testFullStructureTreeCreation() {
    val classFile = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
    Assert.assertNotNull("File $classFilePath not found", classFile)

    var fullyCreated = false
    DefaultClassFileContext.loadSync(project, classFile!!,
                                     {
                                       val tree = StructureTree.create(it, testRootDisposable)
                                       loadAllChildren(tree, tree.getChildren())
                                       fullyCreated = true
                                     },
                                     { throw it })
    Assert.assertTrue(fullyCreated)
  }

  private fun loadAllChildren(tree: StructureTree, children: List<TreeNode>?) {
    children?.forEach { child ->
      if (child is ValueNode) {
        testValueNode(child)
      }

      // Calling `child.getChildren()` may not trigger the loading of all
      // children by the model if [StructureNode#willAlwaysHaveAsyncChildren]
      // is true.
      loadAllChildren(tree, tree.getChildren(child))
    } ?: throw AssertionFailedError("Children not loaded.")
  }

  /**
   * Test generation of the ValueNode values.
   */
  private fun testValueNode(valueNode: ValueNode) {
    structureTreeContextPermutations.forEach { structureTreeContext ->
      valueNode.displayValue(structureTreeContext)
      valueNode.rawValue(structureTreeContext)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
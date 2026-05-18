package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure

import dev.turingcomplete.intellijbytecodeplugin.ClassFileConsumerTestCase
import dev.turingcomplete.intellijbytecodeplugin._ui.DefaultClassFileContext
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode
import javax.swing.tree.TreeNode
import junit.framework.AssertionFailedError
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test tries to parse all classes from the `java.base` module and from the `kotlin-stdlib`
 * into a [StructureTree].
 */
@RunWith(org.junit.runners.Parameterized::class)
class StructureTreeTest(@Suppress("unused") testName: String, classFilePaths: List<String>) :
  ClassFileConsumerTestCase(classFilePaths) {
  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<Any>> = testData()
  }

  // -- Properties
  // -------------------------------------------------------------------------------------------------- //

  private val structureTreeContextPermutations = mutableListOf<StructureTreeContext>()

  // -- Initialization
  // ----------------------------------------------------------------------------------------------
  // //
  // -- Exposed Methods
  // ---------------------------------------------------------------------------------------------
  // //

  override fun setUp() {
    super.setUp()

    val defaultStructureTreeContext = StructureTreeContext(project) {}
    structureTreeContextPermutations.add(defaultStructureTreeContext)
    TypeUtils.TypeNameRenderMode.values()
      .filter { defaultStructureTreeContext.typeNameRenderMode != it }
      .forEach {
        structureTreeContextPermutations.add(
          StructureTreeContext(project) {}.apply { typeNameRenderMode = it }
        )
      }
    MethodDeclarationUtils.MethodDescriptorRenderMode.values()
      .filter { defaultStructureTreeContext.methodDescriptorRenderMode != it }
      .forEach {
        structureTreeContextPermutations.add(
          StructureTreeContext(project) {}.apply { methodDescriptorRenderMode = it }
        )
      }
  }

  @Test
  fun testFullStructureTreeCreation() {
    consumeClassFiles { classFileAsVirtualFile ->
      val classFileContext =
        DefaultClassFileContext(project, ClassFile(classFileAsVirtualFile, null), false)
      val tree = StructureTree(classFileContext, testRootDisposable)
      loadAllChildren(tree, tree.getChildren())
    }
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

  /** Test generation of the ValueNode values. */
  private fun testValueNode(valueNode: ValueNode) {
    structureTreeContextPermutations.forEach { structureTreeContext ->
      valueNode.displayValue(structureTreeContext)
      valueNode.rawValue(structureTreeContext)
    }
  }

  // -- Private Methods
  // ---------------------------------------------------------------------------------------------
  // //
  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //
}

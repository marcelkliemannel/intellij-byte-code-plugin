package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ModuleNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.StructureNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.TextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode

internal class ModuleStructureNode(private val moduleNode: ModuleNode)
  : TextNode("Module Descriptor", AllIcons.Nodes.JavaModule) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    asyncAdd {
      add(ValueNode("Name:", moduleNode.name))
      addAccessNode(moduleNode.access, AccessGroup.MODULE)
      addVersionNode(moduleNode.version)
      addMainClass()
      addPackages()
      addModuleRequires()
      addExportsNode()
      addOpensNode()
      addUsesNode()
      addProvidesNode()
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun addMainClass() {
    moduleNode.mainClass?.let {
      add(ValueNode("Main class:", it))
    }
  }

  private fun addPackages() {
    addTitleNodeWithElements(moduleNode.packages, { TextNode("Packages", AllIcons.Nodes.Package) }) { _, `package` ->
      ValueNode(displayValue = `package`, icon = AllIcons.Nodes.Package)
    }
  }

  private fun addModuleRequires() {
    addTitleNodeWithElements(moduleNode.requires, { TextNode("Requires") }) { _, requires ->
      ValueNode(displayValue = requires.module, icon = AllIcons.Nodes.JavaModule).apply {
        addAccessNode(requires.access, AccessGroup.MODULE_REQUIRES)
        addVersionNode(requires.version)
      }
    }
  }

  private fun StructureNode.addVersionNode(version: String?) {
    version ?: return
    add(ValueNode("Version:", version))
  }

  private fun addExportsNode() {
    addTitleNodeWithElements(moduleNode.exports, { TextNode("Exports") }) { _, exports ->
      createPackageToModulesNode(exports.packaze, exports.access, AccessGroup.MODULE_EXPORTS, exports.modules)
    }
  }

  private fun addOpensNode() {
    addTitleNodeWithElements(moduleNode.opens, { TextNode("Opens") }) { _, opens ->
      createPackageToModulesNode(opens.packaze, opens.access, AccessGroup.MODULE_OPENS, opens.modules)
    }
  }

  private fun createPackageToModulesNode(`package`: String,
                                         access: Int,
                                         accessGroup: AccessGroup,
                                         modules: List<String>?): StructureNode {

    return ValueNode(displayValue = `package`, icon = AllIcons.Nodes.Package).apply {
      addAccessNode(access, accessGroup)
      modules?.takeIf { it.isNotEmpty() }?.let { modules ->
        add(TextNode("to Modules").apply {
          modules.forEach { module -> add(ValueNode(displayValue = module, icon = AllIcons.Nodes.JavaModule)) }
        })
      }
    }
  }

  private fun addUsesNode() {
    addTitleNodeWithElements(moduleNode.uses, { TextNode("Uses") }) { _, uses ->
      ValueNode("Service:", { ctx -> TypeUtils.toReadableName(uses, ctx.typeNameRenderMode) })
    }
  }

  private fun addProvidesNode() {
    addTitleNodeWithElements(moduleNode.provides, { TextNode("Provides") }) { _, provides ->
      val serviceInternalName = provides.service
      ValueNode("Service:",
                { ctx -> TypeUtils.toReadableName(serviceInternalName, ctx.typeNameRenderMode) },
                goToProvider = GoToProvider.Class(serviceInternalName)).apply {

        provides.providers.forEach { provider ->
          add(ValueNode("with Provider:",
                        { ctx -> TypeUtils.toReadableName(provider, ctx.typeNameRenderMode) },
                        goToProvider = GoToProvider.Class(provider)))
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
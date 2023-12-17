package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.jrt.JrtFileSystem
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.util.text.DateFormatUtil
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.bytecode.ClassVersionUtils.toClassVersion
import dev.turingcomplete.intellijbytecodeplugin.bytecode.ClassVersionUtils.toMajorMinorString
import dev.turingcomplete.intellijbytecodeplugin.bytecode.MethodDeclarationUtils
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Opcodes
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.ClassNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HtmlTextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HyperLinkNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.TextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode
import org.apache.commons.io.FileUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.Icon

internal class ClassStructureNode(
  private val classNode: ClassNode,
  private val classFile: ClassFile
) : ValueNode(
  displayValue = { cxt -> TypeUtils.toReadableName(classNode.name, cxt.typeNameRenderMode) },
  icon = determineClassIcon(classNode),
  goToProvider = GoToProvider.Class(classNode.name)
) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val LOG = Logger.getInstance(ClassStructureNode::class.java)

    private val KOTLIN_METADATA_ANNOTATION_DESC = "Lkotlin/Metadata;"

    private fun determineClassIcon(classNode: ClassNode): Icon {
      return when {
        classNode.access and Opcodes.ACC_INTERFACE != 0 -> AllIcons.Nodes.Interface
        classNode.access and Opcodes.ACC_ENUM != 0 -> AllIcons.Nodes.Enum
        classNode.access and Opcodes.ACC_ABSTRACT != 0 -> AllIcons.Nodes.AbstractClass
        classNode.access and Opcodes.ACC_RECORD != 0 -> AllIcons.Nodes.Record
        else -> AllIcons.Nodes.Class
      }
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    asyncAdd(true) {
      addClassVersionNode()
      addAccessNode(classNode.access, if (classNode.module != null) AccessGroup.MODULE else AccessGroup.CLASS)
      addSignatureNode(classNode.signature)
      addSuperNameNode()
      addInterfacesNode()
      addDebugNode()
      addModuleNode()
      addOuterClassNode()
      addOuterMethodNode()
      addAttributesNode(classNode.attrs)
      addInnerClasses()
      addNestNode()
      addKotlinMetadataNode()
      addAnnotationsNode("Annotations", classNode.visibleAnnotations, classNode.invisibleAnnotations)
      addAnnotationsNode("Type Annotations", classNode.visibleTypeAnnotations, classNode.invisibleTypeAnnotations)
      addFieldsNode()
      addMethodsNode()
      addRecordComponentsNode()
      addPermittedSubclassesNode()
      addFileNode(classFile.file, "Class file")
      classFile.sourceFile?.let { addFileNode(it.file, "Source file") }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun addClassVersionNode() {
    add(
      HtmlTextNode(
        "Class version:", toMajorMinorString(classNode.version),
        postFix = toClassVersion(classNode.version)?.let {
          "<span class=\"contextHelp\">${it.specification}</span>"
        },
        icon = AllIcons.FileTypes.Java
      )
    )
  }

  private fun addSuperNameNode() {
    classNode.superName?.let { superName ->
      add(
        ValueNode(
          "Super:",
          { ctx -> TypeUtils.toReadableName(superName, ctx.typeNameRenderMode) },
          icon = AllIcons.Hierarchy.Supertypes,
          goToProvider = GoToProvider.Class(superName)
        )
      )
    }
  }

  private fun addDebugNode() {
    if (classNode.sourceFile == null && classNode.sourceDebug == null) {
      return
    }

    add(TextNode("Debug Information", AllIcons.Actions.StartDebugger).apply {
      classNode.sourceFile?.let { sourceFile ->
        add(ValueNode("Source file:", sourceFile, icon = AllIcons.FileTypes.Java))
      }

      classNode.sourceDebug?.let { sourceDebug ->
        add(ValueNode("Source debug:", sourceDebug))
      }
    })
  }

  private fun addModuleNode() {
    classNode.module?.let {
      add(ModuleStructureNode(it))
    }
  }

  private fun addOuterClassNode() {
    classNode.outerClass?.let { outerClass ->
      add(
        ValueNode(
          "Outer class:", { ctx -> TypeUtils.toReadableName(outerClass, ctx.typeNameRenderMode) },
          goToProvider = GoToProvider.Class(outerClass)
        )
      )
    }
  }

  private fun addOuterMethodNode() {
    classNode.outerMethod?.let { outerMethod ->
      add(ValueNode("Outer method:",
                    { ctx ->
                      MethodDeclarationUtils.toReadableDeclaration(
                        outerMethod,
                        classNode.outerMethodDesc ?: "",
                        classNode.outerMethodDesc ?: "",
                        ctx.typeNameRenderMode,
                        ctx.methodDescriptorRenderMode,
                        true
                      )
                    },
                    { ctx ->
                      MethodDeclarationUtils.toReadableDeclaration(
                        outerMethod,
                        classNode.outerMethodDesc ?: "",
                        classNode.outerMethodDesc ?: "",
                        ctx.typeNameRenderMode,
                        ctx.methodDescriptorRenderMode,
                        false
                      )
                    }
      ))
    }
  }

  private fun addNestNode() {
    if (classNode.nestHostClass == null && classNode.nestMembers?.isNotEmpty() != true) {
      return
    }

    add(TextNode("Nest", AllIcons.Actions.GroupBy).apply {
      classNode.nestHostClass?.let { nestHostClass ->
        add(
          ValueNode(
            "Nest host:",
            { ctx -> TypeUtils.toReadableName(nestHostClass, ctx.typeNameRenderMode) },
            goToProvider = GoToProvider.Class(nestHostClass)
          )
        )
      }

      addTitleNodeWithElements(classNode.nestMembers, { TextNode("Nest Members") }) { _, nestMember ->
        ValueNode(
          displayValue = { ctx -> TypeUtils.toReadableName(nestMember, ctx.typeNameRenderMode) },
          goToProvider = GoToProvider.Class(nestMember)
        )
      }
    })
  }

  private fun addKotlinMetadataNode() {
    val kotlinMetadataAnnotation = classNode.visibleAnnotations?.find { it.desc == KOTLIN_METADATA_ANNOTATION_DESC }
      ?: return

    val fieldNameToValueList: List<Pair<Any, Any?>> = kotlinMetadataAnnotation.values.toList().chunked(2).map { Pair(it[0], it[1]) }
    add(KotlinMetadataStructureNode(fieldNameToValueList))
  }

  private fun addInnerClasses() {
    addTitleNodeWithElements(classNode.innerClasses, { TextNode("Inner classes", AllIcons.Nodes.Class) }) { _, innerClass ->
      ValueNode(
        displayValue = { ctx -> TypeUtils.toReadableName(innerClass.name, ctx.typeNameRenderMode) },
        icon = AllIcons.Nodes.Class,
        goToProvider = GoToProvider.Class(innerClass.name)
      )
    }
  }

  private fun addMethodsNode() {
    addTitleNodeWithElements(classNode.methods, { TextNode("Methods", AllIcons.Nodes.Method) }) { _, method ->
      MethodStructureNode(method, classNode)
    }
  }

  private fun addInterfacesNode() {
    addTitleNodeWithElements(classNode.interfaces, { TextNode("Interfaces", AllIcons.Nodes.Interface) }) { _, `interface` ->
      ValueNode(
        displayValue = { ctx -> TypeUtils.toReadableName(`interface`, ctx.typeNameRenderMode) },
        icon = AllIcons.Nodes.Interface,
        goToProvider = GoToProvider.Class(`interface`)
      )
    }
  }

  private fun addFieldsNode() {
    addTitleNodeWithElements(classNode.fields, { TextNode("Fields", AllIcons.Nodes.Field) }) { _, field ->
      FieldStructureNode(field)
    }
  }

  private fun addRecordComponentsNode() {
    addTitleNodeWithElements(classNode.recordComponents, { TextNode("Record Components", AllIcons.Nodes.Record) }) { _, recordComponent ->
      TextNode(recordComponent.name, icon = AllIcons.Nodes.Record).apply {
        asyncAdd {
          recordComponent.descriptor?.let { ValueNode("Descriptor:", it) }
          addSignatureNode(recordComponent.signature)
          addAnnotationsNode("Annotations", recordComponent.visibleAnnotations, recordComponent.invisibleAnnotations)
          addAnnotationsNode("Type Annotations", recordComponent.visibleTypeAnnotations, recordComponent.invisibleTypeAnnotations)
          addAttributesNode(recordComponent.attrs)
        }
      }
    }
  }

  private fun addPermittedSubclassesNode() {
    addTitleNodeWithElements(classNode.permittedSubclasses, { TextNode("Permitted subclasses", AllIcons.General.OverridingMethod) }) { _, subclass ->
      ValueNode(
        displayValue = { ctx -> TypeUtils.toReadableName(subclass, ctx.typeNameRenderMode) },
        icon = AllIcons.General.OverridingMethod,
        goToProvider = GoToProvider.Class(subclass)
      )
    }
  }

  private fun addFileNode(virtualFile: VirtualFile, title: String) {
    if (!virtualFile.isValid || !virtualFile.exists()) {
      return
    }

    val isEntryInArchive = virtualFile.fileSystem is ArchiveFileSystem
    val nioPath = virtualFile.fileSystem.getNioPath(virtualFile)
    if (!isEntryInArchive && nioPath == null) {
      return
    }

    add(TextNode(title, AllIcons.FileTypes.Any_type).apply {
      asyncAdd(true) {
        add(virtualFile.toNioPathOrNull()?.let {
          HyperLinkNode(virtualFile.name) { _, _ -> RevealFileAction.openFile(it) }
        } ?: TextNode(virtualFile.name))

        var parentDirectory = virtualFile.parent

        if (isEntryInArchive) {
          val archive = (virtualFile.fileSystem as ArchiveFileSystem).getLocalByEntry(virtualFile)
          parentDirectory = archive?.parent
          val archiveTypeName = when (virtualFile.fileSystem) {
            is JarFileSystem -> "JAR"
            is JrtFileSystem -> "Java Runtime Image"
            else -> "archive"
          }
          add(ValueNode("Entry of $archiveTypeName:", archive?.name ?: "Unknown"))
        }

        addNioPathNodes(nioPath)

        if (parentDirectory != null && parentDirectory.isInLocalFileSystem) {
          add(HyperLinkNode("Open enclosing directory") { _, _ -> BrowserUtil.browse(parentDirectory) })
        }
      }
    })
  }

  private fun TextNode.addNioPathNodes(nioPath: Path?) {
    nioPath ?: return

    try {
      add(ValueNode("Owner:", Files.getOwner(nioPath).name))

      val attributes = Files.readAttributes(nioPath, BasicFileAttributes::class.java)
      add(ValueNode("Size:", FileUtils.byteCountToDisplaySize(attributes.size())))
      add(ValueNode("Creation time:", DateFormatUtil.formatDateTime(attributes.creationTime().toMillis())))
      add(ValueNode("Last modified time:", DateFormatUtil.formatDateTime(attributes.lastModifiedTime().toMillis())))
      add(ValueNode("Last access time:", DateFormatUtil.formatDateTime(attributes.lastAccessTime().toMillis())))

      val writable = Files.isWritable(nioPath)
      val writableValue = if (writable) "File is writeable" else "File is readonly"
      val writableIcon = if (writable) AllIcons.Ide.Readwrite else AllIcons.Ide.Readonly
      add(ValueNode(displayValue = writableValue, icon = writableIcon))
    } catch (e: IOException) {
      LOG.warn("Failed to read parameters of file: $nioPath", e)
      add(TextNode("Failed to read file parameters: ${e.message}"))
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
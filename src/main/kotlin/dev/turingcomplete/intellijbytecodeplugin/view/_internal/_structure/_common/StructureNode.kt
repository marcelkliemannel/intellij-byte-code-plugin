package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.tree.LeafState
import dev.turingcomplete.intellijbytecodeplugin.bytecode.AccessGroup
import dev.turingcomplete.intellijbytecodeplugin.bytecode.TypeUtils
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Attribute
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.tree.AnnotationNode
import dev.turingcomplete.intellijbytecodeplugin.tool._internal.SignatureParserTool
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.GoToProvider
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure.StructureTreeContext
import org.apache.commons.text.StringEscapeUtils
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.tree.DefaultMutableTreeNode

internal abstract class StructureNode(val goToProvider: GoToProvider? = null)
  : DefaultMutableTreeNode(), LeafState.Supplier {

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private var componentValid = false
  private var asyncAddChildrenInExecution = AtomicBoolean()
  private var asyncAddChildren: (() -> Unit)? = null
  private var willAlwaysHaveAsyncChildren = false

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun component(selected: Boolean, context: StructureTreeContext): JComponent {
    val component = component(selected, context, componentValid)
    componentValid = true
    return component
  }

  protected abstract fun component(selected: Boolean, context: StructureTreeContext, componentValid: Boolean): JComponent

  /**
   * Gets the text which is used for the search inside the structure tree.
   *
   * @return null, if this node is not searchable.
   */
  abstract fun searchText(context: StructureTreeContext): String?

  fun invalidateComponent() {
    componentValid = false
  }

  override fun getLeafState(): LeafState {
    return if (asyncAddChildren != null) {
      if (willAlwaysHaveAsyncChildren) LeafState.NEVER else LeafState.ASYNC
    }
    else {
      LeafState.DEFAULT
    }
  }

  /**
   * Returns true if async loading is in process.
   */
  fun asyncLoadChildren(workAsync: Boolean): Boolean {
    return when {
      asyncAddChildren == null -> {
        // No children to load
        false
      }

      asyncAddChildrenInExecution.getAndSet(true) -> {
        // Already in progress
        true
      }

      else -> {
        // Load children
        if (workAsync) {
          ApplicationManager.getApplication().executeOnPooledThread {
            try {
              asyncAddChildren!!()
            }
            finally {
              asyncAddChildren = null
              asyncAddChildrenInExecution.set(false)
            }
          }
          true
        }
        else {
          asyncAddChildren!!()
          asyncAddChildren = null
          false
        }
      }
    }
  }

  /**
   * @param willAlwaysHaveAsyncChildren if true, the collapse/expand icon will
   * always be shown, without triggering the costly calculation of the number
   * of children.
   */
  fun asyncAdd(willAlwaysHaveAsyncChildren: Boolean = false, asyncAddChildren: (() -> Unit)) {
    this.willAlwaysHaveAsyncChildren = willAlwaysHaveAsyncChildren
    this.asyncAddChildren = asyncAddChildren
  }

  fun <T : Any> addTitleNodeWithElements(elements: List<T>?,
                                         createTitleNode: () -> TextNode,
                                         addElementsAsync: Boolean = false,
                                         mapElement: (Int, T) -> StructureNode?) {

    if (!elements.isNullOrEmpty()) {
      val addElements: TextNode.() -> Unit = {
        elements.mapIndexedNotNull(mapElement).forEach { elementNode ->
          add(elementNode)
        }
      }
      add(createTitleNode().apply {
        if (addElementsAsync) asyncAdd(true) { addElements() } else addElements()
      })
    }
  }

  fun addAttributesNode(attributes: List<Attribute>?) {
    addTitleNodeWithElements(attributes, { TextNode("Attributes", AllIcons.Nodes.ObjectTypeAttribute) }) { _, attribute ->
      ValueNode(displayValue = attribute.type)
    }
  }

  fun addAccessNode(access: Int, accessGroup: AccessGroup) {
    add(
      HtmlTextNode(
        "Access:",
        { ctx -> if (ctx.showAccessAsHex) "0x${Integer.toHexString(access).uppercase(Locale.getDefault())}" else access.toString() },
        postFix = "<span class=\"contextHelp\">${accessGroup.toReadableAccess(access).joinToString(", ")}</span>",
        icon = AllIcons.Nodes.RwAccess
      )
    )
  }

  fun addSignatureNode(signature: String?) {
    if (signature == null) {
      return
    }

    add(TextNode("Signature", AllIcons.Nodes.Type).apply {
      asyncAdd {
        add(ValueNode("Raw:", StringEscapeUtils.escapeHtml4(signature), signature))

        val parsingResult = SignatureParserTool.parseSignature(signature)
        if (parsingResult.error == null) {
          val parsedSignature = parsingResult.signature
          if (parsedSignature.returnType != null) {
            add(ValueNode("Return type:", StringEscapeUtils.escapeHtml4(parsedSignature.returnType), parsedSignature.returnType))
          }
          if (parsedSignature.formalTypeParameter != null) {
            add(ValueNode("Type parameter:", StringEscapeUtils.escapeHtml4(parsedSignature.formalTypeParameter), parsedSignature.formalTypeParameter))
          }
          add(ValueNode("Declaration:", StringEscapeUtils.escapeHtml4(parsedSignature.declaration), parsedSignature.declaration))
          if (parsedSignature.exceptions != null) {
            add(ValueNode("Exceptions:", StringEscapeUtils.escapeHtml4(parsedSignature.exceptions), parsedSignature.exceptions))
          }
        }
      }
    })
  }

  fun addAnnotationsNode(title: String,
                         visibleAnnotations: List<AnnotationNode>?,
                         invisibleAnnotations: List<AnnotationNode>?) {

    if (visibleAnnotations.isNullOrEmpty() && invisibleAnnotations.isNullOrEmpty()) {
      return
    }

    add(TextNode(title, AllIcons.Nodes.Annotationtype).apply {
      asyncAdd(true) {
        visibleAnnotations?.forEach { add(createAnnotationNode(it)) }
        invisibleAnnotations?.forEach { add(createAnnotationNode(it, " <span class=\"contextHelp\">invisible</span>")) }
      }
    })
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createAnnotationNode(annotation: AnnotationNode, postFix: String? = null): StructureNode {
    val values = annotation.values?.let { values ->
      generateSequence(0) { if ((it + 2) < values.size) it + 2 else null }
        .map { "${values[it]} = ${formatAnnotationValue(values[it + 1])}" }
        .joinToString(", ", prefix = "(", postfix = ")")
    } ?: ""
    val internalName = Type.getType(annotation.desc).internalName
    return HtmlTextNode(
      displayValue = { ctx -> TypeUtils.toReadableName(internalName, ctx.typeNameRenderMode) + values },
      postFix = postFix,
      icon = AllIcons.Nodes.Annotationtype,
      goToProvider = GoToProvider.Class(internalName)
    )
  }

  private fun formatAnnotationValue(value: Any?): String {
    return when (value) {
      null -> "null"
      is Collection<*> -> "[${value.joinToString(", ") { formatAnnotationValue(it) }}]"
      is Array<*> -> "[${value.joinToString(", ") { formatAnnotationValue(it) }}]"
      else -> value.toString()
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
}

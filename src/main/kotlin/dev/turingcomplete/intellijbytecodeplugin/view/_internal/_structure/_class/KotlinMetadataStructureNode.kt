package dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._class

import com.intellij.ui.IconManager
import com.intellij.util.asSafely
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.HtmlTextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.TextNode
import dev.turingcomplete.intellijbytecodeplugin.view._internal._structure._common.ValueNode
import org.apache.commons.text.StringEscapeUtils

internal class KotlinMetadataStructureNode(
  private val fieldNameToValueList: List<Pair<Any, Any?>>
) : TextNode("Kotlin Metadata", kotlinIcon) {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    asyncAdd(true) {
      fieldNameToValueList
        .sortedWith(compareBy { fieldNamesOrder.indexOf(it.toString()) })
        .map { it.first to it.second }
        .forEach { (fieldName, value) ->
          when (fieldName) {
            "k" -> addKindMetadataNode(value)
            "mv" -> addVersionNode("Metadata Version:", value)
            "bv" -> addVersionNode("Byte Code Version:", value)
            "d1" -> addDataNode("Data 1", value)
            "d2" -> addDataNode("Data 2", value)
            "xs" -> add(ValueNode("Extra String:", value?.toString() ?: "null"))
            "pn" -> add(ValueNode("Package name:", value?.toString() ?: "null"))
            "xi" -> addExtraIntNode(value)
            else -> add(ValueNode("$fieldName:", value?.toString() ?: "null"))
          }
        }
    }
  }

  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //

  private fun addVersionNode(preFix: String, value: Any?) {
    add(
      ValueNode(
        preFix = preFix,
        displayValue =
          value.asSafely<ArrayList<Int>>()?.joinToString(".") ?: value?.toString() ?: "null",
      )
    )
  }

  private fun addDataNode(title: String, value: Any?) {
    val values =
      value.asSafely<ArrayList<String>>()?.map {
        if (it.isNotBlank()) {
          TextNode(StringEscapeUtils.escapeJava(it))
        } else {
          HtmlTextNode(displayValue = "<i>Blank value</i>", rawValue = "")
        }
      }
    if (!values.isNullOrEmpty()) {
      add(TextNode(title).apply { values.forEach { add(it) } })
    }
  }

  private fun addKindMetadataNode(value: Any?) {
    val contextHelp =
      when (value) {
        1 -> "Class"
        2 -> "File"
        3 -> "Synthetic class"
        4 -> "Multi-file class facade"
        5 -> "Multi-file class part"
        else -> null
      }
    add(
      HtmlTextNode(
        preFix = "Kind:",
        displayValue = { _ -> value?.toString() ?: "null" },
        postFix = contextHelp?.let { "<span class=\"contextHelp\">${it}</span>" },
      )
    )
  }

  private fun addExtraIntNode(value: Any?) {
    val valueInt = value?.asSafely<Int>() ?: 0
    val values =
      extraIntMappings.mapNotNull { (bit, contextHelp) ->
        if ((valueInt and (1 shl bit)) != 0) {
          HtmlTextNode(
            displayValue = { _ -> bit.toString() },
            postFix = "<span class=\"contextHelp\">${contextHelp}</span>",
          )
        } else {
          null
        }
      }
    add(
      HtmlTextNode(
          preFix = "Extra Int:",
          displayValue = { _ -> valueInt.toString() },
          postFix = "<span class=\"contextHelp\">${Integer.toBinaryString(valueInt)}</span>",
        )
        .apply { values.forEach { add(it) } }
    )
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val fieldNamesOrder = listOf("k", "mv", "bv", "d1", "d2", "xs", "pn", "xi")

    private val kotlinIcon =
      IconManager.getInstance()
        .getIcon(
          "dev/turingcomplete/intellijbytecodeplugin/icon/kotlin.svg",
          ClassStructureNode::class.java.classLoader,
        )

    private val extraIntMappings =
      mapOf(
        1 to "Multi-file class facade or part",
        2 to
          "This class file is compiled by a pre-release version of Kotlin and is not visible to release versions",
        3 to "Strict metadata version semantics",
        4 to
          "This class file is compiled with the new Kotlin compiler backend (JVM IR) introduced in Kotlin 1.4",
        5 to "This class file has stable metadata and ABI",
        6 to "This class file is compiled with the new Kotlin compiler frontend (FIR)",
        7 to
          "This class is used in the scope of an inline function and implicitly part of the public ABI",
      )
  }
}

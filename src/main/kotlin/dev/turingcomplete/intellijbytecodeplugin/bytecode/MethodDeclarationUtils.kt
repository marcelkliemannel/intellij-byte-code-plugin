package dev.turingcomplete.intellijbytecodeplugin.bytecode

import com.intellij.openapi.util.text.StringUtil
import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type

object MethodDeclarationUtils {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun toReadableDeclaration(
    name: String,
    descriptor: String,
    ownerInternalName: String,
    typeNameRenderMode: TypeUtils.TypeNameRenderMode,
    methodDescriptorRenderMode: MethodDescriptorRenderMode,
    formatWithHtml: Boolean,
  ): String {

    return when (methodDescriptorRenderMode) {
      MethodDescriptorRenderMode.DESCRIPTOR ->
        "<html><b>${StringUtil.escapeXmlEntities(name)}</b>${descriptor}</html>"
      MethodDescriptorRenderMode.DECLARATION ->
        when (name) {
          "<clinit>" ->
            if (formatWithHtml) "<html><b>Static Initializer</b></html>" else "Static Initializer"
          "<init>" -> {
            if (formatWithHtml) {
              "<html><b>${TypeUtils.toReadableName(ownerInternalName, TypeUtils.TypeNameRenderMode.SIMPLE)}</b>" +
                "(${toReadableParameters(Type.getMethodType(descriptor), typeNameRenderMode)})</html>"
            } else {
              TypeUtils.toReadableName(ownerInternalName, TypeUtils.TypeNameRenderMode.SIMPLE) +
                "(${toReadableParameters(Type.getMethodType(descriptor), typeNameRenderMode)})"
            }
          }
          else -> {
            val methodType = Type.getMethodType(descriptor)
            if (formatWithHtml) {
              "<html>${TypeUtils.toReadableType(methodType.returnType, typeNameRenderMode)} " +
                "<b>$name</b>(${toReadableParameters(methodType, typeNameRenderMode)})</html>"
            } else {
              "${TypeUtils.toReadableType(methodType.returnType, typeNameRenderMode)} " +
                "$name(${toReadableParameters(methodType, typeNameRenderMode)})"
            }
          }
        }
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun toReadableParameters(
    methodType: Type,
    typeNameRenderMode: TypeUtils.TypeNameRenderMode,
  ): String {
    return methodType.argumentTypes.joinToString(", ") {
      TypeUtils.toReadableType(it, typeNameRenderMode)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  enum class MethodDescriptorRenderMode(val title: String) {
    DESCRIPTOR("Method Descriptor"),
    DECLARATION("Method Declaration"),
  }
}

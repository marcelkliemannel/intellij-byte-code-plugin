package dev.turingcomplete.intellijbytecodeplugin.bytecode

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type

object TypeUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun toReadableName(internalName: String, renderMode: TypeNameRenderMode): String {
    return when(renderMode) {
      TypeNameRenderMode.INTERNAL -> internalName
      TypeNameRenderMode.QUALIFIED -> internalName.replace("/", ".")
      TypeNameRenderMode.SIMPLE -> {
        val lastSlashIndex = internalName.lastIndexOf("/")
        return if (lastSlashIndex > 0) internalName.substring(lastSlashIndex + 1) else internalName
      }
    }
  }

  fun toReadableType(typeDescriptor: String, renderMode: TypeNameRenderMode): String {
    return toReadableType(Type.getType(typeDescriptor), renderMode)
  }

  fun toReadableType(type: Type, renderMode: TypeNameRenderMode): String {
    return when (type.sort) {
      Type.VOID -> "void";
      Type.BOOLEAN -> "boolean";
      Type.CHAR -> "char";
      Type.BYTE -> "byte";
      Type.SHORT -> "short";
      Type.INT -> "int";
      Type.FLOAT -> "float";
      Type.LONG -> "long";
      Type.DOUBLE -> "double";
      Type.ARRAY -> "${toReadableType(type.elementType, renderMode)}${"[]".repeat(type.dimensions)}"
      else -> toReadableName(type.internalName, renderMode)
    }
  }
  
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class TypeNameRenderMode(val title: String) {
    INTERNAL("Internal Name"),
    QUALIFIED("Qualified Class Name"),
    SIMPLE("Simple Class Name")
  }
}

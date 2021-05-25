package dev.turingcomplete.intellijbytecodeplugin.asm

import dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm.Type

object AsmTypeUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun toReadableTypeName(internalName: String, renderMode: TypeNameRenderMode): String {
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
      else -> toReadableTypeName(type.internalName, renderMode)
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

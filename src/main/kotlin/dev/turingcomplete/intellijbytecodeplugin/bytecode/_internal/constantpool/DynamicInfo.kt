package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal abstract class DynamicInfo(dataInputStream: DataInputStream, type: String)
  : ConstantPoolInfo(type, readValues(dataInputStream)) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    fun readValues(dataInputStream: DataInputStream): List<Value> {
      // The bootstrapMethodAttrIndex is NOT a reference into the constant pool.
      return listOf(PlainValue("bootstrap_method_attr_index", dataInputStream.readUnsignedShort().toString()),
                    ResolvableIndexValue("name_and_type", dataInputStream.readUnsignedShort()))
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Simple(dataInputStream: DataInputStream) : DynamicInfo(dataInputStream, "Dynamic")

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Invoke(dataInputStream: DataInputStream) : DynamicInfo(dataInputStream, "InvokeDynamic")
}
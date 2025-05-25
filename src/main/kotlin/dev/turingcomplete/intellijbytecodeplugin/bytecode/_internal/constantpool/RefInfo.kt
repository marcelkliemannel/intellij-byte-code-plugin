package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal abstract class RefInfo(dataInputStream: DataInputStream, type: String) :
  ConstantPoolInfo(type, readValues(dataInputStream)) {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    fun readValues(dataInputStream: DataInputStream): List<Value> {
      return listOf(
        ResolvableIndexValue("class", dataInputStream.readUnsignedShort()),
        ResolvableIndexValue("name_and_type", dataInputStream.readUnsignedShort()),
      )
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  internal class Field(dataInputStream: DataInputStream) : RefInfo(dataInputStream, "Fieldref")

  internal class Method(dataInputStream: DataInputStream) : RefInfo(dataInputStream, "Methodref")

  internal class InterfaceMethod(dataInputStream: DataInputStream) :
    RefInfo(dataInputStream, "InterfaceMethodref")
}

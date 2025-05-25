package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal class NameAndTypeInfo(dataInputStream: DataInputStream)
  : ConstantPoolInfo("NameAndType", readValues(dataInputStream)) {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    fun readValues(dataInputStream: DataInputStream): List<Value> {
      return listOf(ResolvableIndexValue("name", dataInputStream.readUnsignedShort()),
                    ResolvableIndexValue("descriptor", dataInputStream.readUnsignedShort()))
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

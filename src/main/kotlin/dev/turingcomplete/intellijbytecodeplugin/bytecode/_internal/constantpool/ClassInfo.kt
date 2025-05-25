package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal class ClassInfo(dataInputStream: DataInputStream) : ConstantPoolInfo("Class", readValues(dataInputStream)) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    fun readValues(dataInputStream: DataInputStream): List<Value> {
      return listOf(ResolvableIndexValue("name", dataInputStream.readUnsignedShort()))
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

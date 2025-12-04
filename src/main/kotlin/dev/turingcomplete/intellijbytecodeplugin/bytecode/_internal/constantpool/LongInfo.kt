package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal class LongInfo(dataInputStream: DataInputStream) :
  ConstantPoolInfo("Long", readValues(dataInputStream), 2) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    fun readValues(dataInputStream: DataInputStream): List<Value> {
      return listOf(PlainValue(value = ConstantPoolUtils.readLong(dataInputStream).toString()))
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

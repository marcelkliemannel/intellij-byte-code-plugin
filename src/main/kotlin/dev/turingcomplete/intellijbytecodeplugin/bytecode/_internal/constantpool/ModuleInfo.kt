package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal class ModuleInfo(dataInputStream: DataInputStream)
  : ConstantPoolInfo("Module_info", readValues(dataInputStream)) {

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

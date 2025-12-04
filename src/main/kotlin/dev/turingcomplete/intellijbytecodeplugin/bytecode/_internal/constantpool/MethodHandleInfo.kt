package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream

internal class MethodHandleInfo(dataInputStream: DataInputStream) :
  ConstantPoolInfo("MethodHandle", readValues(dataInputStream)) {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private val REFERENCE_KINDS =
      mapOf(
        Pair(1, "REF_getField"),
        Pair(2, "REF_getStatic"),
        Pair(3, "REF_putField"),
        Pair(4, "REF_putStatic"),
        Pair(5, "REF_invokeVirtual"),
        Pair(6, "REF_invokeStatic"),
        Pair(7, "REF_invokeSpecial"),
        Pair(8, "REF_newInvokeSpecial"),
        Pair(9, "REF_invokeInterface"),
      )

    fun readValues(dataInputStream: DataInputStream): List<Value> {
      val referenceKind: Int = dataInputStream.readUnsignedByte()
      val referenceKindText = REFERENCE_KINDS[referenceKind]
      return listOf(
        PlainValue("reference_kind=", "$referenceKind ($referenceKindText)"),
        ResolvableIndexValue("reference", dataInputStream.readUnsignedShort()),
      )
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

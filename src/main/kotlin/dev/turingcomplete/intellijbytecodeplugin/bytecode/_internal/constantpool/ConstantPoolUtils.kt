package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import java.io.DataInputStream
import java.nio.ByteBuffer


object ConstantPoolUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun readLong(dataInputStream: DataInputStream): Long {
    return readTwoInts(dataInputStream, java.lang.Long.BYTES).getLong(0)
  }

  fun readDouble(dataInputStream: DataInputStream): Double {
    return readTwoInts(dataInputStream, java.lang.Double.BYTES).getDouble(0)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun readTwoInts(dataInputStream: DataInputStream, capacity: Int) : ByteBuffer {
    return ByteBuffer.allocate(capacity).run {
      putInt(dataInputStream.readInt())
      putInt(dataInputStream.readInt())
      flip()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
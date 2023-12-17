package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import java.io.DataInputStream

internal class ConstantPool(val entries: List<ConstantPoolInfo>) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val MAGIC_BYTES: UInt = 3405691582.toUInt()

    private val TAG_INFO_CREATORS = mapOf<Int, (DataInputStream) -> ConstantPoolInfo>(
            Pair(1) { dataInputStream -> Utf8Info(dataInputStream) },
            Pair(3) { dataInputStream -> IntegerInfo(dataInputStream) },
            Pair(4) { dataInputStream -> FloatInfo(dataInputStream) },
            Pair(5) { dataInputStream -> LongInfo(dataInputStream) },
            Pair(6) { dataInputStream -> DoubleInfo(dataInputStream) },
            Pair(7) { dataInputStream -> ClassInfo(dataInputStream) },
            Pair(8) { dataInputStream -> StringInfo(dataInputStream) },
            Pair(9) { dataInputStream -> RefInfo.Field(dataInputStream) },
            Pair(10) { dataInputStream -> RefInfo.Method(dataInputStream) },
            Pair(11) { dataInputStream -> RefInfo.InterfaceMethod(dataInputStream) },
            Pair(12) { dataInputStream -> NameAndTypeInfo(dataInputStream) },
            Pair(15) { dataInputStream -> MethodHandleInfo(dataInputStream) },
            Pair(16) { dataInputStream -> MethodTypeInfo(dataInputStream) },
            Pair(17) { dataInputStream -> DynamicInfo.Simple(dataInputStream) },
            Pair(18) { dataInputStream -> DynamicInfo.Invoke(dataInputStream) },
            Pair(19) { dataInputStream -> ModuleInfo(dataInputStream) },
            Pair(20) { dataInputStream -> PackageInfo(dataInputStream) })

    fun create(classFile: ClassFile): ConstantPool {
      DataInputStream(classFile.file.inputStream).use { dataInputStream ->
        // Validate magic bytes
        val magicBytes = dataInputStream.readInt().toUInt()
        if (magicBytes != MAGIC_BYTES) {
          throw IllegalArgumentException("Missing expected magic bytes.")
        }

        // Read major and minor version
        dataInputStream.readInt()

        // Read entries
        val entries = mutableListOf<ConstantPoolInfo>()
        val constantPoolCount = dataInputStream.readUnsignedShort()
        var index = 1
        while (index <= constantPoolCount - 1) {
          val tag = dataInputStream.readUnsignedByte()
          val constantPoolInfo = TAG_INFO_CREATORS[tag]?.invoke(dataInputStream)
                                 ?: throw IllegalStateException("Unknown constant pool entry tag: $tag (already read $index out of ${constantPoolCount - 1})")
          entries.add(constantPoolInfo)
          val usedConstantPoolIndices = constantPoolInfo.usedConstantPoolIndices
          if (usedConstantPoolIndices > 1) {
            for (ignore in 1 until usedConstantPoolIndices) {
              entries.add(UnusableInfo())
            }
          }
          index += usedConstantPoolIndices
        }
        return ConstantPool(entries)
      }
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  /**
   * The constant pool starts at index one, but the underling structure used in
   * this class at 0. To correctly resolve referenced indices from entries this
   * method must be used.
   *
   * If its required to work on the 0-indexed structure, the property [entries]
   * is to be used.
   */
  fun getReference(index: Int): ConstantPoolInfo {
    return entries.getOrNull(index - 1)
           ?: throw IllegalArgumentException("Unknown constant pool entry index: $index")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
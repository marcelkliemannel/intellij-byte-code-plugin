package dev.turingcomplete.intellijbytecodeplugin.bytecode._internal.constantpool

internal abstract class ConstantPoolInfo(val type: String,
                                         private val values: List<Value>,
                                         val usedConstantPoolIndices: Int = 1) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val unresolvedDisplayText : String by lazy {
    values.joinToString(", ") { it.createUnresolvedDisplayText() }
  }
  private var resolvedDisplayText : String? = null

  val goToIndices : List<Int> by lazy { values.mapNotNull { it.goToIndex() }.toList() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun resolvedDisplayText(constantPool: ConstantPool): String {
    if (resolvedDisplayText == null) {
      resolvedDisplayText = values.joinToString(", ") { it.createResolvedDisplayText(constantPool) }
    }
    return resolvedDisplayText!!
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal abstract class Value {

    abstract fun createUnresolvedDisplayText(): String

    open fun createResolvedDisplayText(constantPool: ConstantPool) = createUnresolvedDisplayText()

    open fun goToIndex() : Int? = null
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal open class PlainValue(private val name: String? = null, private val value: String) : Value() {

    override fun createUnresolvedDisplayText(): String {
      return "${if (name != null) "$name=" else ""}$value}"
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  internal open class ResolvableIndexValue(private val name: String, private val index: Int) : Value() {

    override fun createUnresolvedDisplayText(): String = "${name}_index=$index"

    override fun createResolvedDisplayText(constantPool: ConstantPool): String {
      val resolvedIndexValue = constantPool.getReference(index).resolvedDisplayText(constantPool)
      return "$name=$resolvedIndexValue"
    }

    override fun goToIndex(): Int? = index
  }
}
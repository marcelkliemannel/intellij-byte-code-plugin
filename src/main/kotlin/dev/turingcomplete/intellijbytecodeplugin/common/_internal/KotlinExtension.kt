package dev.turingcomplete.intellijbytecodeplugin.common._internal

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exported Methods ---------------------------------------------------------------------------------------------- //

fun <T> List<T>.joinAsNaturalLanguage(transform: (T) -> String): String {
  val size = this.size
  if (size == 0) {
    return ""
  }

  val separator = if (size < 2) " and " else ", "
  return this.subList(0, size - 1).joinToString(
    separator,
    postfix = (if (size < 2) "" else " and ") + transform(this[size - 1]),
    transform = transform
  )
}

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //
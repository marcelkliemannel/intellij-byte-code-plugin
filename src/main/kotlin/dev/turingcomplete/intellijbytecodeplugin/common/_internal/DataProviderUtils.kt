package dev.turingcomplete.intellijbytecodeplugin.common._internal

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey

internal object DataProviderUtils {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun <T> getData(dataKey: DataKey<T>, dataContext: DataContext): T {
    return dataKey.getData(dataContext)
      ?: throw IllegalStateException("snh: Missing data from key: ${dataKey.name}")
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}

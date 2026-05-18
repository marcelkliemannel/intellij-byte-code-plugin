package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import org.jetbrains.kotlin.idea.debugger.core.ClassNameProvider

internal object ClassNameProviderConfigurations {
  fun defaultReturningLambdaParentClass(): ClassNameProvider.Configuration =
    ClassNameProvider.Configuration::class
      .java
      .getDeclaredConstructor(Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
      .newInstance(ClassNameProvider.Configuration.DEFAULT.findInlineUseSites, true)
}

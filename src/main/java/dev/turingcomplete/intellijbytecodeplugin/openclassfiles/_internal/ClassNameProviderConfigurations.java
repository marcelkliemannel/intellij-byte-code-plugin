package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal;

import org.jetbrains.kotlin.idea.debugger.core.ClassNameProvider;

final class ClassNameProviderConfigurations {

  private ClassNameProviderConfigurations() {}

  static ClassNameProvider.Configuration defaultReturningLambdaParentClass() {
    return new ClassNameProvider.Configuration(
        ClassNameProvider.Configuration.Companion.getDEFAULT().getFindInlineUseSites(), true);
  }
}

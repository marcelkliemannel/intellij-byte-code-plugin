package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.PrepareReason
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.PrepareReason.MISSING
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesPreparatorService.PrepareReason.OUT_DATED
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ClassFilesPreparatorServiceTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @ParameterizedTest
  @MethodSource("prepareReasonQuestionTestVectors")
  fun `Given variations of source and class file names, When generation the PrepareReason question, Then get the expected text`(
    preparationReason: PrepareReason,
    sourceFileNamesToClassFilesNames: Map<String, List<String>>,
    expectedQuestion: String
  ) {
    val actualQuestion = preparationReason.question(sourceFileNamesToClassFilesNames)
    assertThat(actualQuestion).isEqualTo(expectedQuestion)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    @JvmStatic
    fun prepareReasonQuestionTestVectors(): List<Arguments> = listOf(
      arguments(MISSING, mapOf("Foo.kt" to listOf("A.class")), "The class file 'A.class' for the source file 'Foo.kt' is missing. Should it be compiled?"),
      arguments(MISSING, mapOf("Foo.kt" to listOf("A.class", "B.class")), "The class files 'A.class' and 'B.class' for the source file 'Foo.kt' are missing. Should it be compiled?"),
      arguments(MISSING, mapOf("Foo.kt" to listOf("A.class", "B.class", "C.class")), "The class files 'A.class', 'B.class' and 'C.class' for the source file 'Foo.kt' are missing. Should it be compiled?"),
      arguments(MISSING, mapOf("Foo.kt" to listOf("A.class"), "Bar.kt" to listOf("A.class"), "Baz.kt" to listOf("A.class")), "The class files 'A.class', 'A.class' and 'A.class' for the source files 'Foo.kt', 'Bar.kt' and 'Baz.kt' are missing. Should they be compiled?"),
      arguments(OUT_DATED, mapOf("Foo.kt" to listOf("A.class")), "The source file 'Foo.kt' is outdated. Should it be compiled?"),
      arguments(OUT_DATED, mapOf("Foo.kt" to listOf("A.class", "B.class")), "The source file 'Foo.kt' are outdated. Should it be compiled?"),
      arguments(OUT_DATED, mapOf("Foo.kt" to listOf("A.class"), "Bar.kt" to listOf("A.class"), "Baz.kt" to listOf("A.class")), "The source files 'Foo.kt', 'Bar.kt' and 'Baz.kt' are outdated. Should they be compiled?"),
    )
  }
}
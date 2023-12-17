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
      arguments(
        MISSING,
        mapOf("Foo.kt" to listOf("A.class")),
        "<html> The class file 'A.class' for the source file 'Foo.kt' is missing. Should it be compiled? </html>"
      ),
      arguments(
        MISSING,
        mapOf("Foo.kt" to listOf("A.class", "B.class")),
        "<html> The class files <ul><li>A.class</li><li>B.class</li></ul> for the source file 'Foo.kt' are missing. Should it be compiled? </html>"
      ),
      arguments(
        MISSING,
        mapOf("Foo.kt" to listOf("A.class", "B.class", "C.class")),
        "<html> The class files <ul><li>A.class</li><li>B.class</li><li>C.class</li></ul> for the source file 'Foo.kt' are missing. Should it be compiled? </html>"
      ),
      arguments(
        MISSING,
        mapOf("Foo.kt" to listOf("A.class"), "Bar.kt" to listOf("A.class"), "Baz.kt" to listOf("A.class")),
        "<html> The class files <ul><li>A.class</li><li>A.class</li><li>A.class</li></ul> for the source files <ul><li>Foo.kt</li><li>Bar.kt</li><li>Baz.kt</li></ul> are missing. Should they be compiled? </html>"
      ),
      arguments(
        OUT_DATED,
        mapOf("Foo.kt" to listOf("A.class")),
        "<html> The source file 'Foo.kt' is outdated. Should it be compiled? </html>"
      ),
      arguments(
        OUT_DATED,
        mapOf("Foo.kt" to listOf("A.class", "B.class")),
        "<html> The source file 'Foo.kt' are outdated. Should it be compiled? </html>"
      ),
      arguments(
        OUT_DATED,
        mapOf("Foo.kt" to listOf("A.class"), "Bar.kt" to listOf("A.class"), "Baz.kt" to listOf("A.class")),
        "<html> The source files <ul><li>Foo.kt</li><li>Bar.kt</li><li>Baz.kt</li></ul> are outdated. Should they be compiled? </html>"
      ),
    )
  }
}
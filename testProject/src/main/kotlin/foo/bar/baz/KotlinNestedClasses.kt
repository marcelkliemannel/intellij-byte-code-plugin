// FileTestVector{baseFqClassNames: foo.bar.baz.KotlinNestedClasses, containingFqClassNames: foo.bar.baz.KotlinNestedClasses|foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$1|foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$LocalClass|foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$myObject$1|foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$myObject$1|foo.bar.baz.KotlinNestedClasses$Inner|foo.bar.baz.KotlinNestedClasses$Nested|foo.bar.baz.KotlinNestedClasses$Nested$Companion|foo.bar.baz.KotlinNestedClasses$Companion }
package foo.bar.baz;

import java.util.stream.IntStream

class KotlinNestedClasses {

  fun method() {
    // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses}
  }

  fun `kotlin lambda`(input: String) {
    // The SDK will return #lambdas$result$1 as a class name but the Kotlin
    // compiler will optimize this code without an additional class.
    val result = listOf(1, 2, 3).minOfOrNull {
      // `sourceFileOnly` because decompiler can't restore method body
      // PsiElementTestVector{reference: LAMBDA|KotlinNestedClasses#kotlin lambda, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$kotlin lambda$result$1[foo.bar.baz.KotlinNestedClasses], sourceFileOnly: true}
      input.length + it
    }
  }

  fun `java lambda in kotlin code`(input: String) {
    IntStream.of(1, 2, 3).forEach {
      // `sourceFileOnly` because decompiler can't restore method body
      // PsiElementTestVector{reference: LAMBDA|KotlinNestedClasses#java lambda in kotlin code, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$java lambda in kotlin code$1[foo.bar.baz.KotlinNestedClasses], sourceFileOnly: true}
      println(input.length + it)
    }
  }

  fun `method with local and anonymous class`() {
    val myObject = object {
      fun methodInObjectWithVariable() {
        // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#methodInObjectWithVariable, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$myObject$1[foo.bar.baz.KotlinNestedClasses], sourceFileOnly: true}
      }
    }

    class LocalClass {
      fun methodInLocalClass() {
        // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#methodInLocalClass, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$LocalClass, sourceFileOnly: true}
      }
    }

    object: Runnable {
      override fun run() {
        // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#run, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$method with local and anonymous class$1[foo.bar.baz.KotlinNestedClasses], sourceFileOnly: true}
      }
    }
  }

  inner class Inner {

    fun method() {
      // PsiElementTestVector{reference: METHOD|KotlinNestedClasses$Inner#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$Inner}
    }
  }

  interface Nested {

    private fun method() {
      // PsiElementTestVector{reference: METHOD|KotlinNestedClasses$Nested#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$Nested[foo.bar.baz.KotlinNestedClasses$Nested$DefaultImpls]}
    }

    companion object {

      fun method() {
        // PsiElementTestVector{reference: METHOD|KotlinNestedClasses$Nested$Companion#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$Nested$Companion}
      }
    }
  }

  companion object {

    fun method() {
      // PsiElementTestVector{reference: METHOD|KotlinNestedClasses$Companion#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$Companion}
    }
  }
}
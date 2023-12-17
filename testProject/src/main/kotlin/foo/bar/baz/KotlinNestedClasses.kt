// FileTestVector{baseFqClassNames: foo.bar.baz.KotlinNestedClasses, containingFqClassNames: foo.bar.baz.KotlinNestedClasses|foo.bar.baz.KotlinNestedClasses$methodWithLocalClass$1|foo.bar.baz.KotlinNestedClasses$Inner|foo.bar.baz.KotlinNestedClasses$Nested|foo.bar.baz.KotlinNestedClasses$Nested$Companion|foo.bar.baz.KotlinNestedClasses$Companion }
package foo.bar.baz;

class KotlinNestedClasses {

  fun method() {
    // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses}
  }

  fun methodWithLocalClass() {
    object: Runnable {
      override fun run() {
        // PsiElementTestVector{reference: METHOD|KotlinNestedClasses#run, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses, sourceFileOnly: true}
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
      // PsiElementTestVector{reference: METHOD|KotlinNestedClasses$Nested#method, baseFqClassName: foo.bar.baz.KotlinNestedClasses, expectedFqClassNames: foo.bar.baz.KotlinNestedClasses$Nested}
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
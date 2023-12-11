// FileTestVector{baseFqClassNames: KotlinFirst|KotlinSecond, containingFqClassNames: KotlinFirst|KotlinSecond }
// PsiElementTestVector{reference: BEGIN_OF_FILE, baseFqClassName: KotlinFirst, expectedFqClassNames: KotlinFirst|KotlinSecond, sourceFileOnly: true}

class KotlinFirst {

  fun method() {
    // PsiElementTestVector{reference: METHOD|KotlinFirst#method, baseFqClassName: KotlinFirst, expectedFqClassNames: KotlinFirst}
  }
}

class KotlinSecond {

  fun method() {
    // PsiElementTestVector{reference: METHOD|KotlinSecond#method, baseFqClassName: KotlinSecond, expectedFqClassNames: KotlinSecond}
  }
}

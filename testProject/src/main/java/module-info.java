// FileTestVector{baseFqClassNames: module-info, containingFqClassNames: module-info }
module myModule {
  // If the test project gets opened with a real IntelliJ instance, this line
  // is required to make the project buildable. However, the unit tests will
  // fail if it is present since it can't find the module on the module path.
  // This is something, that should be investigated further...
  //requires kotlin.stdlib;

  exports foo.bar; // PsiElementTestVector{reference:MODULE_EXPORTS|foo.bar, baseFqClassName:module-info, expectedFqClassNames:module-info}
}
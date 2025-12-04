package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.compiled.ClassFileDecompilers.Full
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.utils.io.deleteRecursively
import com.intellij.util.asSafely
import com.intellij.util.lang.UrlClassLoader
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFileCandidates.Companion.fromAbsolutePaths
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesFinderService.Result
import dev.turingcomplete.intellijbytecodeplugin.view._internal._decompiler.DecompilerUtils
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.stream.Stream
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.classIdIfNonLocal
import org.jetbrains.kotlin.idea.base.util.sdk
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes2
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource

class ClassFilesFinderServiceTest {
  // -- Properties
  // -------------------------------------------------------------------------------------------------- //

  private val project: Project
    get() = myExecutionTestCase.project

  private val classFilesFinderService: ClassFilesFinderService
    get() = project.getService(ClassFilesFinderService::class.java)

  private val moduleOutputDir: Path
    get() = myExecutionTestCase.moduleOutputDir

  private val module: Module
    get() = myExecutionTestCase.module

  // -- Initialization
  // ----------------------------------------------------------------------------------------------
  // //
  // -- Exported Methods
  // -------------------------------------------------------------------------------------------- //

  @Test
  fun `Given a source file, When calling fileCanBeAnalysed, Then get the expected result`() {
    val sourceVirtualFile = getSingleTestVector().sourceVirtualFile
    assertThat(sourceVirtualFile.extension).isIn("kt", "java")

    val actualResult = ClassFilesFinderService.fileCanBeAnalysed(sourceVirtualFile, project)

    assertThat(actualResult).isTrue()
  }

  @Test
  fun `Given a class file, When calling fileCanBeAnalysed, Then get the expected result`() {
    val classVirtualFile =
      getSingleTestVector().baseFqClassNames.first().let {
        VirtualFileManager.getInstance().findFileByNioPath(toClassFileInModuleOutputDir(it))!!
      }
    assertThat(classVirtualFile.extension).isEqualTo("class")

    val actualResult = ClassFilesFinderService.fileCanBeAnalysed(classVirtualFile, project)

    assertThat(actualResult).isTrue()
  }

  // -- Private Methods
  // ---------------------------------------------------------------------------------------------
  // //

  private fun getSingleTestVector() =
    myExecutionTestCase.fileTestVectors.first {
      it.sourceVirtualFile.name == "JavaNestedClasses.java"
    }

  private fun toClassFileInModuleOutputDir(fqClassName: String) =
    moduleOutputDir.resolve(fqClassName.toRelativeFilePath("class"))

  private fun String.toRelativeFilePath(extension: String): Path =
    FileSystems.getDefault().getPath("${this.replace('.', '/')}.$extension")

  private fun VirtualFile.toPsiFile(language: Language): PsiFile =
    invokeReadActionAndWait {
      if (this.extension == "class") {
        decompile(language)
      } else {
        PsiManager.getInstance(project).findViewProvider(this)!!.getPsi(language)
      }
    }!!

  private fun VirtualFile.decompile(language: Language): PsiFile {
    assertThat(this.extension).isEqualTo("class")
    return DecompilerUtils.findDecompilersForFile(this)
      .first { it is Full }
      .let { (it as Full).createFileViewProvider(this, PsiManager.getInstance(project), true) }
      .getPsi(language)!!
  }

  private fun isSourceMirrorAvailableInLibraryClassFile(relativeSourceFilePath: Path): Boolean {
    // The `ClassFilesFinderService` can't determine the source file for a
    // module descriptor based on the class file. The service gets the
    // path from the `sourceMirror`, which is only available for `ClsClassImpl`.
    return relativeSourceFilePath.nameWithoutExtension != "module-info"
  }

  private fun absoluteCandidate(
    fqClassName: String,
    fallbackFqClassNames: List<String>,
    toAbsolutePath: (String) -> Path,
  ) =
    (listOf(fqClassName) + fallbackFqClassNames)
      .map { toAbsolutePath(it) }
      .toList()
      .let { fromAbsolutePaths(*it.toTypedArray()) }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  /** Tests [ClassFilesFinderService.findByVirtualFiles] */
  @Nested
  inner class FindByVirtualFiles {

    @ParameterizedTest
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      fileTestVector: FileTestVector
    ) {
      val actualResult =
        classFilesFinderService.findByVirtualFiles(listOf(fileTestVector.sourceVirtualFile))

      // The `ClassFilesFinderService` will produce a `ClassFilePreparationTask`
      // for an empty Kotlin file even though the compiler will not provide
      // such a file. As of now, this is a known limitation since it would
      // require to do a complex check if the file does not contain any
      // compilable elements.
      val expectedResult =
        if (!fileTestVector.sourceFileOnly || fileTestVector.emptyKotlinFile) {
          Result(
            classFilesToPrepare =
              fileTestVector.baseFqClassNames
                .map { fqClassName ->
                  ClassFilesPreparatorService.ClassFilePreparationTask(
                    compilerOutputClassFileCandidates =
                      fromAbsolutePaths(toClassFileInModuleOutputDir(fqClassName)),
                    sourceFile =
                      SourceFile.CompilableSourceFile(fileTestVector.sourceVirtualFile, module),
                  )
                }
                .toMutableList()
          )
        } else {
          Result.withErrorNotProcessableFile(fileTestVector.sourcePsiFile)
        }

      assertThat(actualResult)
        .describedAs(fileTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      baseFqClassName: String
    ) {
      val classVirtualFile =
        VirtualFileManager.getInstance()
          .findFileByNioPath(toClassFileInModuleOutputDir(baseFqClassName))

      val actualResult = classFilesFinderService.findByVirtualFiles(listOf(classVirtualFile!!))

      val expectedClassFile = ClassFile(file = classVirtualFile, sourceFile = null)
      val expectedResult = Result(classFilesToOpen = mutableListOf(expectedClassFile))

      assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      fileTestVector: FileTestVector
    ) {
      val sourceFileFromLibrary =
        myExecutionTestCase.getSourceVirtualFileFromLibrary(fileTestVector.relativeSourceFilePath)
      val actualResult = classFilesFinderService.findByVirtualFiles(listOf(sourceFileFromLibrary))

      val expectedResult =
        if (!fileTestVector.sourceFileOnly) {
          Result(
            classFilesToOpen =
              fileTestVector.baseFqClassNames
                .map { fqClassName ->
                  ClassFile(
                    file =
                      myExecutionTestCase.getClassVirtualFileFromLibrary(
                        fqClassName.toRelativeFilePath("class")
                      ),
                    sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary),
                  )
                }
                .toMutableList()
          )
        } else {
          Result.withErrorNotProcessableFile(fileTestVector.sourcePsiFile)
        }

      assertThat(actualResult)
        .describedAs(fileTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      baseFqClassName: String,
      fileTestVector: FileTestVector,
    ) {
      val classFileFromLibraryWithClassesAndSources =
        myExecutionTestCase.getClassVirtualFileFromLibrary(
          baseFqClassName.toRelativeFilePath("class")
        )
      val actualResult =
        classFilesFinderService.findByVirtualFiles(
          listOf(classFileFromLibraryWithClassesAndSources)
        )

      val expectedClassFile =
        ClassFile(
          file = classFileFromLibraryWithClassesAndSources,
          sourceFile =
            if (isSourceMirrorAvailableInLibraryClassFile(fileTestVector.relativeSourceFilePath)) {
              SourceFile.NonCompilableSourceFile(
                myExecutionTestCase.getSourceVirtualFileFromLibrary(
                  fileTestVector.relativeSourceFilePath
                )
              )
            } else {
              null
            },
        )
      val expectedResult = Result(classFilesToOpen = mutableListOf(expectedClassFile))

      assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
    }

    @Test
    fun `Given a light virtual file (which does not have a physical file), When using findByVirtualFiles, Then get the expected error`() {
      val lightVirtualFile = LightVirtualFile("Test.java", "class Foo() {}")

      val actualResult = classFilesFinderService.findByVirtualFiles(listOf(lightVirtualFile))

      val expectedResult =
        Result.withError(
          "Couldn't determine the expected class file path for source file 'Test.java'.\n" +
            "Try to recompile the project or, if it's a non-project Java source file, " +
            "compile the file separately and open the resulting class file directly."
        )

      assertThat(actualResult).isEqualTo(expectedResult)
    }
  }

  /** Tests [ClassFilesFinderService.findByPsiFiles] */
  @Nested
  inner class FindByPsiFiles {

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file, When using findByPsiFiles, Then get the correct class files to prepare`(
      fileTestVector: FileTestVector
    ) {
      val actualResult =
        classFilesFinderService.findByPsiFiles(listOf(fileTestVector.sourcePsiFile))

      val expectedResult =
        if (!fileTestVector.sourceFileOnly || fileTestVector.emptyKotlinFile) {
          Result(
            classFilesToPrepare =
              fileTestVector.baseFqClassNames
                .map { fqClassName ->
                  ClassFilesPreparatorService.ClassFilePreparationTask(
                    compilerOutputClassFileCandidates =
                      fromAbsolutePaths(toClassFileInModuleOutputDir(fqClassName)),
                    sourceFile =
                      SourceFile.CompilableSourceFile(fileTestVector.sourceVirtualFile, module),
                  )
                }
                .toMutableList()
          )
        } else {
          Result.withErrorNotProcessableFile(fileTestVector.sourcePsiFile)
        }

      assertThat(actualResult)
        .describedAs(fileTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file, When using findByPsiFiles, Then get the correct class files to prepare`(
      baseFqClassName: String
    ) {
      val classVirtualFile =
        VirtualFileManager.getInstance()
          .findFileByNioPath(toClassFileInModuleOutputDir(baseFqClassName))
      val classPsiFile = runReadAction {
        PsiManager.getInstance(project).findFile(classVirtualFile!!)
      }
      val actualResult = classFilesFinderService.findByPsiFiles(listOf(classPsiFile!!))

      val expectedClassFile = ClassFile(file = classVirtualFile!!, sourceFile = null)
      val expectedResult = Result(classFilesToOpen = mutableListOf(expectedClassFile))

      assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file from a library, When using findByPsiFiles, Then get the correct class files to open`(
      fileTestVector: FileTestVector
    ) {
      val sourceFileFromLibrary =
        myExecutionTestCase.getSourceVirtualFileFromLibrary(fileTestVector.relativeSourceFilePath)
      val sourcePsiFileFromLibrary = runReadAction {
        PsiManager.getInstance(project).findFile(sourceFileFromLibrary)
      }
      val actualResult = classFilesFinderService.findByPsiFiles(listOf(sourcePsiFileFromLibrary!!))

      val expectedResult =
        if (!fileTestVector.sourceFileOnly) {
          Result(
            classFilesToOpen =
              fileTestVector.baseFqClassNames
                .map { fqClassName ->
                  ClassFile(
                    file =
                      myExecutionTestCase.getClassVirtualFileFromLibrary(
                        fqClassName.toRelativeFilePath("class")
                      ),
                    sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary),
                  )
                }
                .toMutableList()
          )
        } else {
          Result.withErrorNotProcessableFile(fileTestVector.sourcePsiFile)
        }

      assertThat(actualResult)
        .describedAs(fileTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file from a library, When using findByPsiFiles, Then get the correct class files to open`(
      baseFqClassName: String,
      fileTestVector: FileTestVector,
    ) {
      val classFileFromLibrary =
        myExecutionTestCase.getClassVirtualFileFromLibrary(
          baseFqClassName.toRelativeFilePath("class")
        )
      val classPsiFileFromLibrary = runReadAction {
        PsiManager.getInstance(project).findFile(classFileFromLibrary)
      }
      val actualResult = classFilesFinderService.findByPsiFiles(listOf(classPsiFileFromLibrary!!))

      val expectedClassFile =
        ClassFile(
          file = classFileFromLibrary,
          sourceFile =
            if (baseFqClassName != "module-info") {
              SourceFile.NonCompilableSourceFile(
                myExecutionTestCase.getSourceVirtualFileFromLibrary(
                  fileTestVector.relativeSourceFilePath
                )
              )
            } else {
              null
            },
        )
      val expectedResult = Result(classFilesToOpen = mutableListOf(expectedClassFile))

      assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
    }

    @Test
    fun `Given a PSI file from a light virtual file (which does not have a physical file), When using findByPsiFiles, Then get the expected error`() {
      val lightVirtualFile = LightVirtualFile("Test.java", "class Foo() {}")
      val lightPsiFile = runReadAction {
        PsiManager.getInstance(project).findFile(lightVirtualFile)!!
      }

      val actualResult = classFilesFinderService.findByPsiFiles(listOf(lightPsiFile))

      val expectedResult =
        Result.withError(
          "Couldn't determine the expected class file path for source file 'Test.java'.\n" +
            "Try to recompile the project or, if it's a non-project Java source file, " +
            "compile the file separately and open the resulting class file directly."
        )

      assertThat(actualResult).isEqualTo(expectedResult)
    }
  }

  /** Tests [ClassFilesFinderService.findByPsiElements] */
  @Nested
  inner class FindByPsiElement {

    @ParameterizedTest
    @ArgumentsSource(SourcePsiElementTestVectors::class)
    fun `Given PSI elements from a source file, When using findByPsiElement, Then get the correct class files to prepare`(
      psiElementTestVector: PsiElementTestVector
    ) {
      val testPsiElement =
        psiElementTestVector.psiElementReference.find(
          psiElementTestVector.sourceVirtualFile.toPsiFile(psiElementTestVector.sourceLanguage)
        )
      val actualResult =
        classFilesFinderService.findByPsiElements(
          mapOf(testPsiElement to psiElementTestVector.sourcePsiFile)
        )

      val expectedClassFilePreparationTasks =
        psiElementTestVector.expectedFqClassNames
          .map { (fqClassName, fallbackFqClassNames) ->
            ClassFilesPreparatorService.ClassFilePreparationTask(
              compilerOutputClassFileCandidates =
                absoluteCandidate(fqClassName, fallbackFqClassNames) {
                  toClassFileInModuleOutputDir(it)
                },
              sourceFile =
                SourceFile.CompilableSourceFile(psiElementTestVector.sourceVirtualFile, module),
            )
          }
          .toMutableList()
      val expectedResult = Result(classFilesToPrepare = expectedClassFilePreparationTasks)

      assertThat(actualResult)
        .describedAs(psiElementTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(ClassPsiElementTestVectors::class)
    fun `Given PSI elements from a class file, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      val classVirtualFile =
        VirtualFileManager.getInstance()
          .findFileByNioPath(toClassFileInModuleOutputDir(psiElementTestVector.baseFqClassName))
      val testPsiElement =
        psiElementTestVector.psiElementReference.find(
          classVirtualFile!!.toPsiFile(psiElementTestVector.sourceLanguage)
        )
      val actualResult =
        classFilesFinderService.findByPsiElements(
          mapOf(testPsiElement to runReadAction { testPsiElement.containingFile })
        )

      val expectedClassFilesToOpen =
        psiElementTestVector.expectedFqClassNames
          .map { (fqClassName, _) ->
            ClassFile(
              file =
                VirtualFileManager.getInstance()
                  .findFileByNioPath(toClassFileInModuleOutputDir(fqClassName))!!,
              sourceFile = null,
            )
          }
          .toMutableList()
      val expectedResult = Result(classFilesToOpen = expectedClassFilesToOpen)

      assertThat(actualResult)
        .describedAs(psiElementTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(SourcePsiElementTestVectors::class)
    fun `Given PSI elements from a source file in a library, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      val sourceFileFromLibrary =
        myExecutionTestCase.getSourceVirtualFileFromLibrary(
          psiElementTestVector.relativeSourceFilePath
        )
      val sourceFileFromLibraryPsiFile = runReadAction {
        PsiManager.getInstance(project).findFile(sourceFileFromLibrary)!!
      }
      val testPsiElement =
        psiElementTestVector.psiElementReference.find(sourceFileFromLibraryPsiFile)
      val actualResult =
        classFilesFinderService.findByPsiElements(
          mapOf(testPsiElement to runReadAction { testPsiElement.containingFile })
        )

      val expectedResult =
        if (
          psiElementTestVector.expectedFqClassNames.isNotEmpty() &&
            !psiElementTestVector.emptyKotlinFile
        ) {
          Result(
            classFilesToOpen =
              psiElementTestVector.expectedFqClassNames
                .map { (fqClassName, fallbackPaths) ->
                  var expectedClassFilePath =
                    myExecutionTestCase.findClassVirtualFileFromLibrary(
                      fqClassName.toRelativeFilePath("class")
                    )
                  if (expectedClassFilePath == null) {
                    expectedClassFilePath =
                      myExecutionTestCase.findClassVirtualFileFromLibrary(
                        fallbackPaths.first().toRelativeFilePath("class")
                      )
                  }
                  ClassFile(
                    file = expectedClassFilePath!!,
                    sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary),
                  )
                }
                .toMutableList()
          )
        } else {
          Result.withErrorNotProcessableFile(psiElementTestVector.sourcePsiFile)
        }

      assertThat(actualResult)
        .describedAs(psiElementTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @ParameterizedTest
    @ArgumentsSource(ClassPsiElementTestVectors::class)
    fun `Given PSI elements from a class file in a library, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      val classFileFromLibrary =
        myExecutionTestCase.getClassVirtualFileFromLibrary(
          psiElementTestVector.baseFqClassName.toRelativeFilePath("class")
        )
      val classFileFromLibraryPsiFile = runReadAction {
        PsiManager.getInstance(project).findFile(classFileFromLibrary)!!
      }
      val testPsiElement =
        psiElementTestVector.psiElementReference.find(classFileFromLibraryPsiFile)
      val actualResult =
        classFilesFinderService.findByPsiElements(
          mapOf(testPsiElement to runReadAction { testPsiElement.containingFile })
        )

      val expectedClassFilesToOpen =
        psiElementTestVector.expectedFqClassNames
          .map { (fqClassName, _) ->
            ClassFile(
              file =
                myExecutionTestCase.getClassVirtualFileFromLibrary(
                  fqClassName.toRelativeFilePath("class")
                ),
              sourceFile =
                if (
                  isSourceMirrorAvailableInLibraryClassFile(
                    psiElementTestVector.relativeSourceFilePath
                  )
                ) {
                  SourceFile.NonCompilableSourceFile(
                    myExecutionTestCase.getSourceVirtualFileFromLibrary(
                      psiElementTestVector.relativeSourceFilePath
                    )
                  )
                } else {
                  null
                },
            )
          }
          .toMutableList()
      val expectedResult = Result(classFilesToOpen = expectedClassFilesToOpen)

      assertThat(actualResult)
        .describedAs(psiElementTestVector.sourceVirtualFile.name)
        .isEqualTo(expectedResult)
    }

    @Test
    fun `Given a PSI file from a light virtual file (which does not have a physical file), When using findByPsiFiles, Then get the expected error`() {
      val lightVirtualFile = LightVirtualFile("Test.java", "class Foo() { void method() {  } }")
      val lightPsiFile = runReadAction {
        PsiManager.getInstance(project).findFile(lightVirtualFile)!!
      }
      val lightPsiElement = PsiMethodReference(listOf("Foo"), "method").find(lightPsiFile)

      val actualResult =
        classFilesFinderService.findByPsiElements(mapOf(lightPsiElement to lightPsiFile))

      val expectedResult =
        Result.withError(
          "Couldn't determine the expected class file path for source file 'Test.java'.\n" +
            "Try to recompile the project or, if it's a non-project Java source file, " +
            "compile the file separately and open the resulting class file directly."
        )

      assertThat(actualResult).isEqualTo(expectedResult)
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  /** Tests [ClassFilesFinderService.findByClassFiles] */
  @Nested
  inner class FindByClassFiles {

    @Test
    fun `Given a ClassFile without a source file, When using findByClassFiles, then get the expected result`() {
      val classVirtualFile =
        getSingleTestVector().baseFqClassNames.first().let {
          VirtualFileManager.getInstance().findFileByNioPath(toClassFileInModuleOutputDir(it))!!
        }

      val actualResult =
        classFilesFinderService.findByClassFiles(listOf(ClassFile(file = classVirtualFile)))

      val expectedResult = Result.withClassFileToOpen(ClassFile(file = classVirtualFile))

      assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `Given a ClassFile with a compilable source file, When using findByClassFiles, then get the expected result`() {
      val testVector = getSingleTestVector()
      val classVirtualFile =
        testVector.baseFqClassNames.first().let {
          VirtualFileManager.getInstance().findFileByNioPath(toClassFileInModuleOutputDir(it))!!
        }

      val actualResult =
        classFilesFinderService.findByClassFiles(
          listOf(
            ClassFile(
              file = classVirtualFile,
              sourceFile =
                SourceFile.CompilableSourceFile(
                  file = testVector.sourceVirtualFile,
                  module = module,
                ),
            )
          )
        )

      val expectedResult =
        Result.withClassFileToPrepare(
          ClassFilesPreparatorService.ClassFilePreparationTask(
            compilerOutputClassFileCandidates = fromAbsolutePaths(classVirtualFile.toNioPath()),
            sourceFile =
              SourceFile.CompilableSourceFile(file = testVector.sourceVirtualFile, module = module),
          )
        )

      assertThat(actualResult).isEqualTo(expectedResult)
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  abstract class PsiReference {

    abstract fun find(psiFile: PsiFile): PsiElement

    protected fun findContainingClassNames(ktElement: KtElement): List<String> {
      val parentClassOrObject =
        ktElement.getParentOfType<KtClassOrObject>(true) ?: return emptyList()
      return findContainingClassNames(parentClassOrObject) +
        (parentClassOrObject.classIdIfNonLocal?.let { listOf(it.shortClassName.asString()) }
          ?: emptyList())
    }

    protected fun findContainingClassNames(psiElement: PsiElement): List<String> {
      val parentPsiClass: PsiClass? = psiElement.getParentOfType<PsiClass>(true)
      if (parentPsiClass != null) {
        return findContainingClassNames(parentPsiClass) +
          (parentPsiClass.classIdIfNonLocal?.let { listOf(it.shortClassName.asString()) }
            ?: emptyList())
      } else {
        val parentKtClassOrObject: KtClassOrObject? =
          psiElement.getParentOfType<KtClassOrObject>(true)
        if (parentKtClassOrObject != null) {
          return findContainingClassNames(parentKtClassOrObject) +
            (parentKtClassOrObject.classIdIfNonLocal?.let { listOf(it.shortClassName.asString()) }
              ?: emptyList())
        }
      }
      return emptyList()
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class PsiLambdaReference(val containingClassNames: List<String>, val methodName: String) :
    PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      psiFile.accept(
        recursivelyVisitPsiElement { element ->
          runReadAction {
            if (element is KtLambdaExpression || element is PsiLambdaExpression) {
              val parentMethod: PsiNamedElement? =
                element.getParentOfTypes2<KtNamedFunction, PsiMethod>().asSafely<PsiNamedElement>()
              if (
                parentMethod != null &&
                  parentMethod.name == methodName &&
                  containingClassNames == findContainingClassNames(parentMethod)
              ) {
                assertThat(result).isNull()
                result = element
              }
            }
          }
          true
        }
      )

      assertThat(result)
        .describedAs("Find lambda in method '$methodName' in file $psiFile")
        .isNotNull()
      return result!!
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class PsiMethodReference(val containingClassNames: List<String>, val methodName: String) :
    PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      psiFile.accept(
        recursivelyVisitPsiElement { element ->
          runReadAction {
            if (
              element is KtNamedFunction &&
                element.name == methodName &&
                containingClassNames == findContainingClassNames(element)
            ) {
              assertThat(result).isNull()
              result = element
            } else if (
              element is PsiMethod &&
                element.name == methodName &&
                containingClassNames == findContainingClassNames(element)
            ) {
              assertThat(result).isNull()
              result = element
            }
          }
          true
        }
      )

      assertThat(result).describedAs("Find method '$methodName' in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class PsiModuleExportsReference(val packageName: String) : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      psiFile.accept(
        recursivelyVisitPsiElement { element ->
          runReadAction {
            if (element is PsiJavaModule) {
              assertThat(result).isNull()
              result = element.exports.find { it.packageName == packageName }
            }
          }
          true
        }
      )

      assertThat(result)
        .describedAs("Find module exports '$packageName' in file $psiFile")
        .isNotNull()
      return result!!
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class PsiEnumValueReference(val valueName: String) : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      psiFile.accept(
        recursivelyVisitPsiElement { element ->
          runReadAction {
            if (element is KtEnumEntry && element.name == valueName) {
              assertThat(result).isNull()
              result = element
            } else if (element is PsiEnumConstant && element.name == valueName) {
              assertThat(result).isNull()
              result = element
            }
          }
          true
        }
      )

      assertThat(result).describedAs("Find enum value '$valueName' in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  class PsiBeginOfFileReference : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      psiFile.accept(
        recursivelyVisitPsiElement { element ->
          result = element
          false
        }
      )

      assertThat(result).describedAs("Find method first element in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  @Ignore
  internal class MyExecutionTestCase : ExecutionTestCase() {

    private val testProjectPath = Path.of("testProject").absolute()
    private val sourceDirs =
      listOf(
        testProjectPath.resolve(Path.of("src", "main", "java")),
        testProjectPath.resolve(Path.of("src", "main", "kotlin")),
      )

    internal lateinit var psiElementTestVectors: List<PsiElementTestVector>
    internal lateinit var fileTestVectors: List<FileTestVector>

    private var inSetUp = true
    private lateinit var sourcesJar: VirtualFile
    private lateinit var classesJar: VirtualFile

    init {
      name = this::class.simpleName
    }

    override fun initOutputChecker(): OutputChecker =
      OutputChecker({ testProjectPath.toString() }, { "out" })

    override fun getTestAppPath(): String = testProjectPath.toString()

    public override fun setUp() {
      // The `super.setUp()` will run `compileProject()` but the language to the
      // module is not set yet. As of IntelliJ 2023.3, the default language will
      // be Java 8, which can't handle certain language constructs, like Java
      // module descriptors.
      inSetUp = true
      testProjectPath.resolve(".idea").takeIf { it.exists() }?.deleteRecursively()
      super.setUp()
      IdeaTestUtil.setModuleLanguageLevel(module, projectLanguageLevel, testRootDisposable)
      addKotlinStdLibrary()
      inSetUp = false

      compileProject()

      addProjectAsLibrary()

      fileTestVectors = collectFileTestVectors()
      assertThat(fileTestVectors).isNotEmpty

      psiElementTestVectors = collectPsiElementTestVectors()
      assertThat(psiElementTestVectors).isNotEmpty
    }

    private fun addKotlinStdLibrary() {
      val kotlinJarFile =
        (Thread.currentThread().contextClassLoader as UrlClassLoader)
          .urls
          .map { File(it.toURI()) }
          .first { it.name.startsWith("kotlin-stdlib-") }
      val kotlinJarVirtualFile =
        invokeReadActionAndWait { VfsUtil.findFileByIoFile(kotlinJarFile, true) }!!
      val kotlinLibrary =
        PsiTestUtil.addProjectLibrary(
          module,
          "kotlin-stdlib",
          listOf(JarFileSystem.getInstance().getRootByLocal(kotlinJarVirtualFile)!!),
          emptyList(),
        )
      Disposer.register(testRootDisposable) {
        runWriteAction {
          LibraryTablesRegistrar.getInstance().getLibraryTable(project).removeLibrary(kotlinLibrary)
        }
      }
    }

    private fun addProjectAsLibrary() {
      val classesAJarPath = tempDir.newPath("classesA.jar", true)
      copyDirToJar(classesAJarPath, listOf(moduleOutputDir))
      classesJar =
        invokeReadActionAndWait { VfsUtil.findFileByIoFile(classesAJarPath.toFile(), true) }!!

      val sourcesAJarPath = tempDir.newPath("sourcesA.jar", true)
      copyDirToJar(sourcesAJarPath, sourceDirs)
      sourcesJar =
        invokeReadActionAndWait { VfsUtil.findFileByIoFile(sourcesAJarPath.toFile(), true) }!!

      // Create library with classes and sources
      val libraryWithClassesAndSources =
        PsiTestUtil.addProjectLibrary(
          module,
          "my-library",
          listOf(JarFileSystem.getInstance().getRootByLocal(classesJar)!!),
          listOf(JarFileSystem.getInstance().getRootByLocal(sourcesJar)!!),
        )
      Disposer.register(testRootDisposable) {
        runWriteAction {
          LibraryTablesRegistrar.getInstance()
            .getLibraryTable(project)
            .removeLibrary(libraryWithClassesAndSources)
        }
      }
    }

    fun getSourceVirtualFileFromLibrary(relativeSourceFilePath: Path): VirtualFile {
      val fileInSourceJar =
        JarFileSystem.getInstance()
          .getJarRootForLocalFile(sourcesJar)
          ?.findFile(relativeSourceFilePath.toString())
      assertThat(fileInSourceJar).describedAs(relativeSourceFilePath.toString()).isNotNull()
      assertThat(runReadAction { LibraryUtil.findLibraryEntry(fileInSourceJar!!, project) })
        .isNotNull()
      return fileInSourceJar!!
    }

    fun getClassVirtualFileFromLibrary(relativeClassFilePath: Path): VirtualFile {
      val fileInClassesJar = findClassVirtualFileFromLibrary(relativeClassFilePath)
      assertThat(fileInClassesJar).describedAs(relativeClassFilePath.toString()).isNotNull()
      assertThat(runReadAction { LibraryUtil.findLibraryEntry(fileInClassesJar!!, project) })
        .isNotNull()
      return fileInClassesJar!!
    }

    fun findClassVirtualFileFromLibrary(relativeClassFilePath: Path): VirtualFile? =
      JarFileSystem.getInstance()
        .getJarRootForLocalFile(classesJar)
        ?.findFile(relativeClassFilePath.toString())

    public override fun tearDown() {
      val projectJdkTable = ProjectJdkTable.getInstance()
      projectJdkTable.allJdks.forEach { invokeWriteActionAndWait { projectJdkTable.removeJdk(it) } }

      try {
        super.tearDown()
      } catch (e: Exception) {
        if (e.message?.contains("hasn't been disposed") != true) {
          throw e
        }
      }
    }

    public override fun getModuleOutputDir(): Path {
      return super.getModuleOutputDir()
    }

    override fun getProjectLanguageLevel(): LanguageLevel {
      val languageLevel = JavaSdk.getInstance().getVersion(module.sdk!!)!!.maxLanguageLevel
      assertThat(languageLevel).isGreaterThanOrEqualTo(LanguageLevel.JDK_1_9)
      return languageLevel
    }

    override fun overrideCompileJdkAndOutput(): Boolean = true

    override fun compileProject() {
      if (!inSetUp) {
        super.compileProject()
      }
    }

    private fun copyDirToJar(jarFilePath: Path, dirs: List<Path>) {
      JarOutputStream(
          Files.newOutputStream(jarFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)
        )
        .use { jar ->
          dirs.forEach { dir ->
            Files.walk(dir).use { paths ->
              paths
                .filter { it.isRegularFile() }
                .forEach { path ->
                  val entry = dir.relativize(path).toString()
                  val jarEntry = JarEntry(entry)
                  jar.putNextEntry(jarEntry)
                  if (Files.isRegularFile(path)) Files.copy(path, jar)
                  jar.closeEntry()
                }
            }
          }
        }
    }

    private fun collectFileTestVectors(): List<FileTestVector> {
      val fileTestVector = mutableListOf<FileTestVector>()

      walkSourceDirs { psiElement, sourceFile, sourceVirtualFile, sourcePsiFile, _ ->
        if (psiElement !is PsiComment) {
          return@walkSourceDirs
        }

        val testVectorMarkerResult =
          Regex("// FileTestVector\\{(.*)}").find(psiElement.text) ?: return@walkSourceDirs
        val testVectorMarker = testVectorMarkerResult.parseTestVectorMarker()
        val containingFqClassNames =
          testVectorMarker["containingFqClassNames"]!!.split("|").filter { it.isNotBlank() }
        val baseFqClassNames =
          testVectorMarker["baseFqClassNames"]!!.split("|").filter { it.isNotBlank() }
        val sourceFileOnly = testVectorMarker["sourceFileOnly"]?.toBoolean() ?: false
        val emptyKotlinFile = testVectorMarker["emptyKotlinFile"]?.toBoolean() ?: false
        val sourceDir = sourceDirs.find { sourceFile.startsWith(it) }!!
        fileTestVector.add(
          FileTestVector(
            title = testVectorMarkerResult.groupValues[1],
            sourceVirtualFile = sourceVirtualFile,
            sourcePsiFile = sourcePsiFile,
            relativeSourceFilePath = sourceVirtualFile.toNioPath().relativeTo(sourceDir),
            containingFqClassNames = containingFqClassNames,
            baseFqClassNames = baseFqClassNames,
            sourceFileOnly = sourceFileOnly,
            emptyKotlinFile = emptyKotlinFile,
          )
        )
      }

      assertThat(fileTestVector).hasSize(countOccurrencesInSourceFiles("FileTestVector"))

      return fileTestVector
    }

    private fun countOccurrencesInSourceFiles(searchString: String): Int =
      sourceDirs.sumOf { sourceDir ->
        var result = 0
        Files.walk(sourceDir).use { paths ->
          paths
            .filter { it.isRegularFile() }
            .forEach { result += it.toFile().readText().split(searchString).size - 1 }
        }
        result
      }

    private fun collectPsiElementTestVectors(): List<PsiElementTestVector> {
      val psiElementTestVectors = mutableListOf<PsiElementTestVector>()

      walkSourceDirs { psiElement, sourceFile, sourceVirtualFile, sourcePsiFile, sourceLanguage ->
        if (psiElement !is PsiComment) {
          return@walkSourceDirs
        }

        val testVectorMarkerResult = Regex("// PsiElementTestVector\\{(.*)}").find(psiElement.text)
        if (testVectorMarkerResult == null) {
          assertThat(psiElement.text).doesNotContain("PsiElementTestVector")
          return@walkSourceDirs
        }

        val testVectorMarker = testVectorMarkerResult.parseTestVectorMarker()
        val baseFqClassName = testVectorMarker["baseFqClassName"]!!
        val expectedFqClassNames =
          Regex("([^\\[|]+)(?:\\[([^]]+)])?")
            .findAll(testVectorMarker["expectedFqClassNames"]!!)
            .associate { match ->
              val key = match.groupValues[1]
              val fallbacks = match.groups[2]?.value?.split('|') ?: emptyList()
              key to fallbacks
            }
        val sourceFileOnly = testVectorMarker["sourceFileOnly"]?.toBoolean() ?: false
        val reference = testVectorMarker["reference"]!!
        val emptyKotlinFile = testVectorMarker["emptyKotlinFile"]?.toBoolean() ?: false
        val psiReference =
          when {
            reference.startsWith("METHOD|") -> {
              val referencePath = reference.substringAfter("|")
              val (fqClassName, methodName) =
                if ('#' in referencePath) referencePath.split("#") else listOf("", referencePath)
              val containingClassNames =
                if ('$' in fqClassName) fqClassName.split('$')
                else (if (fqClassName.isNotBlank()) listOf(fqClassName) else emptyList())
              PsiMethodReference(containingClassNames, methodName)
            }

            reference.startsWith("LAMBDA|") -> {
              val referencePath = reference.substringAfter("|")
              val (fqClassName, methodName) =
                if ('#' in referencePath) referencePath.split("#") else listOf("", referencePath)
              val containingClassNames =
                if ('$' in fqClassName) fqClassName.split('$')
                else (if (fqClassName.isNotBlank()) listOf(fqClassName) else emptyList())
              PsiLambdaReference(containingClassNames, methodName)
            }

            reference == "BEGIN_OF_FILE" -> PsiBeginOfFileReference()

            reference.startsWith("MODULE_EXPORTS|") -> {
              val packageName = reference.substringAfter("|")
              PsiModuleExportsReference(packageName)
            }

            reference.startsWith("ENUM_VALUE|") -> {
              val valueName = reference.substringAfter("|")
              PsiEnumValueReference(valueName)
            }

            else -> Assertions.fail("Can't parse test case comment: ${psiElement.text}")
          }
        val sourceDir = sourceDirs.find { sourceFile.startsWith(it) }!!
        psiElementTestVectors.add(
          PsiElementTestVector(
            title = testVectorMarkerResult.groupValues[1],
            sourceVirtualFile = sourceVirtualFile,
            relativeSourceFilePath = sourceVirtualFile.toNioPath().relativeTo(sourceDir),
            sourcePsiFile = sourcePsiFile,
            sourceLanguage = sourceLanguage,
            psiElementReference = psiReference,
            baseFqClassName = baseFqClassName,
            expectedFqClassNames = expectedFqClassNames,
            sourceFileOnly = sourceFileOnly,
            emptyKotlinFile = emptyKotlinFile,
          )
        )
      }

      assertThat(psiElementTestVectors)
        .hasSize(countOccurrencesInSourceFiles("PsiElementTestVector"))

      return psiElementTestVectors
    }

    private fun MatchResult.parseTestVectorMarker(): Map<String, String> {
      val testVectorMarker =
        this.groupValues[1].split(",").associate {
          val (key, value) = it.split(":")
          key.trim() to value.trim()
        }
      return testVectorMarker
    }

    private fun walkSourceDirs(
      visitPsiElement: (PsiElement, Path, VirtualFile, PsiFile, Language) -> Unit
    ) {
      sourceDirs.forEach { sourceDir ->
        Files.walkFileTree(
          sourceDir,
          object : SimpleFileVisitor<Path>() {
            private val consumableFileExtensions = listOf("java", "kt", "kts")

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
              if (!consumableFileExtensions.contains(file.extension)) {
                return FileVisitResult.CONTINUE
              }

              val sourceVirtualFile = VfsUtil.findFileByIoFile(file.toFile(), true)
              assertThat(sourceVirtualFile).describedAs("Virtual file for $file").isNotNull()
              val sourceLanguage: Language =
                when {
                  sourceVirtualFile!!.isJavaFileType() -> JavaLanguage.INSTANCE
                  sourceVirtualFile.isKotlinFileType() -> KotlinLanguage.INSTANCE
                  else -> throw IllegalArgumentException("Can't determine language of file: $this")
                }
              invokeReadActionAndWait {
                VirtualFileManager.getInstance().syncRefresh()
                var sourcePsiFile: PsiFile? =
                  PsiManager.getInstance(project)
                    .findViewProvider(sourceVirtualFile)!!
                    .getPsi(sourceLanguage)
                if (sourcePsiFile == null || sourceVirtualFile.name.contains("$")) {
                  val containingVirtualFile =
                    sourceVirtualFile.parent?.findFile(
                      "${sourceVirtualFile.name.substringBefore('$')}.class"
                    )
                  if (containingVirtualFile != null) {
                    sourcePsiFile = PsiManager.getInstance(project).findFile(containingVirtualFile)
                  }
                }
                assertThat(sourcePsiFile).describedAs("Find PSI file for: $file").isNotNull()
                val visitor = recursivelyVisitPsiElement { psiElement ->
                  visitPsiElement(
                    psiElement,
                    file,
                    sourceVirtualFile,
                    sourcePsiFile!!,
                    sourceLanguage,
                  )
                  true
                }
                sourcePsiFile!!.accept(visitor)
              }

              return FileVisitResult.CONTINUE
            }
          },
        )
      }
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class FileTestVector(
    val title: String,
    val sourceVirtualFile: VirtualFile,
    val sourcePsiFile: PsiFile,
    val relativeSourceFilePath: Path,
    val containingFqClassNames: List<String>,
    val baseFqClassNames: List<String>,
    val sourceFileOnly: Boolean,
    val emptyKotlinFile: Boolean,
  ) {

    override fun toString(): String = "${sourceVirtualFile.name}: $title"
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  data class PsiElementTestVector(
    val title: String,
    val sourceVirtualFile: VirtualFile,
    val relativeSourceFilePath: Path,
    val sourcePsiFile: PsiFile,
    val sourceLanguage: Language,
    val psiElementReference: PsiReference,
    val baseFqClassName: String,
    val expectedFqClassNames: Map<String, List<String>>,
    val sourceFileOnly: Boolean,
    val emptyKotlinFile: Boolean,
  ) {

    override fun toString(): String = "${sourceVirtualFile.name}: $title"
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  class SourcePsiElementTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> =
      myExecutionTestCase.psiElementTestVectors.map { Arguments.of(it) }.stream()
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  class ClassPsiElementTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> =
      myExecutionTestCase.psiElementTestVectors
        .filter { !it.sourceFileOnly }
        .map { Arguments.of(it) }
        .stream()
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  class SourceFileTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
      @Suppress("DEPRECATION")
      val filterTestVectors =
        context.requiredTestMethod.getAnnotation(FilterTestVectors::class.java)
      return myExecutionTestCase.fileTestVectors
        .filter { testVector ->
          filterTestVectors?.value?.let { testVector.toString().contains(it) } ?: true
        }
        .map { Arguments.of(it) }
        .stream()
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  class ClassFileTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<Arguments> {
      val isOnlyNestedClasses = context.tags.contains(ONLY_NON_NESTED_CLASSES_TAG)
      return myExecutionTestCase.fileTestVectors
        .filter { !it.sourceFileOnly }
        .flatMap { testVector ->
          testVector.containingFqClassNames
            .filter { !isOnlyNestedClasses || !it.contains("$") }
            .map { Arguments.of(it, testVector) }
        }
        .stream()
    }
  }

  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //

  @Deprecated(message = "Should only be used for debugging")
  annotation class FilterTestVectors(val value: String)

  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {

    const val ONLY_NON_NESTED_CLASSES_TAG = "only-non-nested-classes"

    private val myExecutionTestCase = MyExecutionTestCase()

    @BeforeAll
    @JvmStatic
    fun setUp() {
      myExecutionTestCase.setUp()
    }

    @AfterAll
    @JvmStatic
    fun tearDown() {
      myExecutionTestCase.tearDown()
    }

    fun <T> invokeWriteActionAndWait(action: () -> T): T? {
      var result: T? = null
      ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction { result = action() }
      }
      return result
    }

    fun <T> invokeReadActionAndWait(action: () -> T): T? {
      var result: T? = null
      ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runReadAction { result = action() }
      }
      return result
    }

    private fun recursivelyVisitPsiElement(
      visitPsiElement: (PsiElement) -> Boolean
    ): PsiElementVisitor =
      object : PsiElementVisitor() {

        override fun visitElement(element: PsiElement) {
          super.visitElement(element)
          val shouldContinue = visitPsiElement(element)
          if (!shouldContinue) {
            return
          }

          // Using `PsiElement#children` may not include all `PsiComment`
          // elements.
          var psiChild: PsiElement? = runReadAction { element.firstChild }
          while (psiChild != null) {
            psiChild.accept(recursivelyVisitPsiElement(visitPsiElement))
            psiChild = runReadAction { psiChild!!.nextSibling }
          }
        }
      }
  }
}

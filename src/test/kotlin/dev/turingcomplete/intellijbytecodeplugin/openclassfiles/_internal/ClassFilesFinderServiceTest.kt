package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.debugger.impl.OutputChecker
import com.intellij.execution.ExecutionTestCase
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.libraries.LibraryUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.compiled.ClassFileDecompilers.Full
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PsiTestUtil
import dev.turingcomplete.intellijbytecodeplugin.common.ClassFile
import dev.turingcomplete.intellijbytecodeplugin.common.SourceFile
import dev.turingcomplete.intellijbytecodeplugin.view._internal._decompiler.DecompilerUtils
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.base.psi.classIdIfNonLocal
import org.jetbrains.kotlin.idea.base.util.sdk
import org.jetbrains.kotlin.idea.k2.codeinsight.structuralsearch.visitor.KotlinRecursiveElementVisitor
import org.jetbrains.kotlin.idea.util.isJavaFileType
import org.jetbrains.kotlin.idea.util.isKotlinFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.scripting.definitions.StandardScriptDefinition.fileType
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
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
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

class ClassFilesFinderServiceTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val project: Project
    get() = myExecutionTestCase.project

  private val classFilesFinderService: ClassFilesFinderService
    get() = project.getService(ClassFilesFinderService::class.java)

  private val moduleOutputDir: Path
    get() = myExecutionTestCase.moduleOutputDir

  private val module: Module
    get() = myExecutionTestCase.module

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun String.toRelativeFilePath(extension: String): Path =
    FileSystems.getDefault().getPath("${this.replace('.', '/')}.$extension")

  private fun VirtualFile.toPsiFile(language: Language): PsiFile =
    invokeReadActionAndWait {
      if (this.extension == "class") {
        decompile(language)
      }
      else {
        PsiManager.getInstance(project)
          .findViewProvider(this)!!
          .getPsi(language)
      }
    }!!

  private fun VirtualFile.decompile(language: Language): PsiFile {
    assertThat(this.extension).isEqualTo("class")
    return DecompilerUtils.findDecompilersForFile(this)
      .first { it is Full }
      .let { (it as Full).createFileViewProvider(this, PsiManager.getInstance(project), true) }
      .getPsi(language)!!
  }

  fun isSourceMirrorAvailableInLibraryClassFile(relativeSourceFilePath: Path): Boolean {
    // The `ClassFilesFinderService` can't determine the source file for a
    // module descriptor based on the class file. The service gets the
    // path from the `sourceMirror`, which is only available for `ClsClassImpl`.
    return relativeSourceFilePath.nameWithoutExtension != "module-info"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  /**
   * Tests [ClassFilesFinderService.findByVirtualFiles]
   */
  @Nested
  inner class FindByVirtualFiles {

    @ParameterizedTest
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val actualResult = classFilesFinderService.findByVirtualFiles(listOf(fileTestVector.sourceVirtualFile))

        // The `ClassFilesFinderService` will produce a `ClassFilePreparationTask`
        // for an empty Kotlin file even though the compiler will not provide
        // such a file. As of now, this is a known limitation since it would
        // require to do a complex check if the file does not contain any
        // compilable elements.
        val expectedClassFilePreparationTasks = if (!fileTestVector.sourceFileOnly || fileTestVector.emptyKotlinFile) {
          fileTestVector.baseFqClassNames.map { fqClassName ->
            ClassFilesPreparatorService.ClassFilePreparationTask(
              compilerOutputClassFilePath = moduleOutputDir.resolve(fqClassName.toRelativeFilePath("class")),
              sourceFile = SourceFile.CompilableSourceFile(fileTestVector.sourceVirtualFile, module)
            )
          }.toMutableList()
        }
        else {
          mutableListOf()
        }
        val expectedResult = ClassFilesFinderService.Result(classFilesToPrepare = expectedClassFilePreparationTasks)

        assertThat(actualResult).describedAs(fileTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      baseFqClassName: String, fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val classRelativePath = baseFqClassName.toRelativeFilePath("class")
        val classFile = moduleOutputDir.resolve(classRelativePath)
        val classVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(classFile)
        assertThat(classVirtualFile).describedAs("Class file: $classFile").isNotNull()

        val actualResult = classFilesFinderService.findByVirtualFiles(listOf(classVirtualFile!!))

        val expectedClassFile = ClassFile(
          file = VirtualFileManager.getInstance().findFileByNioPath(moduleOutputDir.resolve(classRelativePath))!!,
          sourceFile = null
        )
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = mutableListOf(expectedClassFile))

        assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val sourceFileFromLibrary =
          myExecutionTestCase.getSourceFileFromLibrary(fileTestVector.relativeSourceFilePath)
        val actualResult = classFilesFinderService.findByVirtualFiles(listOf(sourceFileFromLibrary))

        val expectedClassFiles = if (!fileTestVector.sourceFileOnly) {
          fileTestVector.baseFqClassNames.map { fqClassName ->
            ClassFile(
              file = myExecutionTestCase.getClassFileFromLibrary(fqClassName.toRelativeFilePath("class")),
              sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary)
            )
          }.toMutableList()
        }
        else {
          mutableListOf()
        }
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = expectedClassFiles)

        assertThat(actualResult).describedAs(fileTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      baseFqClassName: String, fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val classFileFromLibraryWithClassesAndSources =
          myExecutionTestCase.getClassFileFromLibrary(baseFqClassName.toRelativeFilePath("class"))
        val actualResult = classFilesFinderService.findByVirtualFiles(listOf(classFileFromLibraryWithClassesAndSources))

        val expectedClassFile = ClassFile(
          file = classFileFromLibraryWithClassesAndSources,
          sourceFile = if (isSourceMirrorAvailableInLibraryClassFile(fileTestVector.relativeSourceFilePath)) {
            SourceFile.NonCompilableSourceFile(myExecutionTestCase.getSourceFileFromLibrary(fileTestVector.relativeSourceFilePath))
          }
          else {
            null
          }
        )
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = mutableListOf(expectedClassFile))

        assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
      }
    }
  }

  /**
   * Tests [ClassFilesFinderService.findByPsiFiles]
   */
  @Nested
  inner class FindByPsiFiles {

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val actualResult = classFilesFinderService.findByPsiFiles(listOf(fileTestVector.sourcePsiFile))

        val expectedClassFilePreparationTasks = if (!fileTestVector.sourceFileOnly || fileTestVector.emptyKotlinFile) {
          fileTestVector.baseFqClassNames.map { fqClassName ->
            ClassFilesPreparatorService.ClassFilePreparationTask(
              compilerOutputClassFilePath = moduleOutputDir.resolve(fqClassName.toRelativeFilePath("class")),
              sourceFile = SourceFile.CompilableSourceFile(fileTestVector.sourceVirtualFile, module)
            )
          }.toMutableList()
        }
        else {
          mutableListOf()
        }
        val expectedResult = ClassFilesFinderService.Result(classFilesToPrepare = expectedClassFilePreparationTasks)

        assertThat(actualResult).describedAs(fileTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file, When using findByVirtualFiles, Then get the correct class files to prepare`(
      baseFqClassName: String, fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val classRelativePath = baseFqClassName.toRelativeFilePath("class")
        val classFile = moduleOutputDir.resolve(classRelativePath)
        val classVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(classFile)
        assertThat(classVirtualFile).describedAs("Class file: $classFile").isNotNull()
        val classPsiFile = PsiManager.getInstance(project).findFile(classVirtualFile!!)
        assertThat(classPsiFile).describedAs("Class file: $classFile").isNotNull()

        val actualResult = classFilesFinderService.findByPsiFiles(listOf(classPsiFile!!))

        val expectedClassFile = ClassFile(
          file = VirtualFileManager.getInstance().findFileByNioPath(moduleOutputDir.resolve(classRelativePath))!!,
          sourceFile = null
        )
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = mutableListOf(expectedClassFile))

        assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(SourceFileTestVectors::class)
    fun `Given a source file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val sourceFileFromLibrary =
          myExecutionTestCase.getSourceFileFromLibrary(fileTestVector.relativeSourceFilePath)
        val sourcePsiFileFromLibrary = PsiManager.getInstance(project).findFile(sourceFileFromLibrary)
        assertThat(sourcePsiFileFromLibrary).describedAs("Class file: $sourceFileFromLibrary").isNotNull()
        val actualResult = classFilesFinderService.findByPsiFiles(listOf(sourcePsiFileFromLibrary!!))

        val expectedClassFiles = if (!fileTestVector.sourceFileOnly) {
          fileTestVector.baseFqClassNames.map { fqClassName ->
            ClassFile(
              file = myExecutionTestCase.getClassFileFromLibrary(fqClassName.toRelativeFilePath("class")),
              sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary)
            )
          }.toMutableList()
        }
        else {
          mutableListOf()
        }
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = expectedClassFiles)

        assertThat(actualResult).describedAs(fileTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @Tag(ONLY_NON_NESTED_CLASSES_TAG)
    @ArgumentsSource(ClassFileTestVectors::class)
    fun `Given a class file from a library, When using findByVirtualFiles, Then get the correct class files to open`(
      baseFqClassName: String, fileTestVector: FileTestVector
    ) {
      invokeReadActionAndWait {
        val classFileFromLibrary =
          myExecutionTestCase.getClassFileFromLibrary(baseFqClassName.toRelativeFilePath("class"))
        val classPsiFileFromLibrary = PsiManager.getInstance(project).findFile(classFileFromLibrary)
        assertThat(classPsiFileFromLibrary).describedAs("Class file: $classPsiFileFromLibrary").isNotNull()
        val actualResult = classFilesFinderService.findByPsiFiles(listOf(classPsiFileFromLibrary!!))

        val expectedClassFile = ClassFile(
          file = classFileFromLibrary,
          sourceFile = if (baseFqClassName != "module-info") {
            SourceFile.NonCompilableSourceFile(myExecutionTestCase.getSourceFileFromLibrary(fileTestVector.relativeSourceFilePath))
          }
          else {
            null
          }
        )
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = mutableListOf(expectedClassFile))

        assertThat(actualResult).describedAs(baseFqClassName).isEqualTo(expectedResult)
      }
    }
  }

  /**
   * Tests [ClassFilesFinderService.findByPsiElement]
   */
  @Nested
  inner class FindByPsiElement {

    @ParameterizedTest
    @ArgumentsSource(SourcePsiElementTestVectors::class)
    fun `Given PSI elements from a source file, When using findByPsiElement, Then get the correct class files to prepare`(
      psiElementTestVector: PsiElementTestVector
    ) {
      invokeReadActionAndWait {
        val testPsiElement =
          psiElementTestVector.psiElementReference.find(psiElementTestVector.sourceVirtualFile.toPsiFile(psiElementTestVector.sourceLanguage))
        val actualResult = classFilesFinderService.findByPsiElement(testPsiElement, psiElementTestVector.sourcePsiFile)

        val expectedClassFilePreparationTasks = psiElementTestVector.expectedFqClassNames.map { fqClassName ->
          ClassFilesPreparatorService.ClassFilePreparationTask(
            compilerOutputClassFilePath = moduleOutputDir.resolve(fqClassName.toRelativeFilePath("class")),
            sourceFile = SourceFile.CompilableSourceFile(psiElementTestVector.sourceVirtualFile, module)
          )
        }.toMutableList()
        val expectedResult = ClassFilesFinderService.Result(classFilesToPrepare = expectedClassFilePreparationTasks)

        assertThat(actualResult).describedAs(psiElementTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClassPsiElementTestVectors::class)
    fun `Given PSI elements from a class file, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      invokeReadActionAndWait {
        val classFile = moduleOutputDir.resolve(psiElementTestVector.baseFqClassName.toRelativeFilePath("class"))
        val classVirtualFile = VirtualFileManager.getInstance().findFileByNioPath(classFile)
        assertThat(classVirtualFile).describedAs("Class file: $classFile").isNotNull()
        val testPsiElement = psiElementTestVector.psiElementReference.find(classVirtualFile!!.toPsiFile(psiElementTestVector.sourceLanguage))
        val actualResult = classFilesFinderService.findByPsiElement(testPsiElement, testPsiElement.containingFile)

        val expectedClassFilesToOpen = psiElementTestVector.expectedFqClassNames.map { fqClassName ->
          ClassFile(
            file = VirtualFileManager.getInstance().findFileByNioPath(moduleOutputDir.resolve(fqClassName.toRelativeFilePath("class")))!!,
            sourceFile = null
          )
        }.toMutableList()
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = expectedClassFilesToOpen)

        assertThat(actualResult).describedAs(psiElementTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(SourcePsiElementTestVectors::class)
    fun `Given PSI elements from a source file in a library, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      invokeReadActionAndWait {
        val sourceFileFromLibrary =
          myExecutionTestCase.getSourceFileFromLibrary(psiElementTestVector.relativeSourceFilePath)
        val testPsiElement = psiElementTestVector.psiElementReference.find(PsiManager.getInstance(project).findFile(sourceFileFromLibrary)!!)
        val actualResult = classFilesFinderService.findByPsiElement(testPsiElement, testPsiElement.containingFile)

        val expectedClassFilesToOpen = psiElementTestVector.expectedFqClassNames.map { fqClassName ->
          val expectedClassFileFromLibrary = myExecutionTestCase.getClassFileFromLibrary(fqClassName.toRelativeFilePath("class"))
          ClassFile(
            file = expectedClassFileFromLibrary,
            sourceFile = SourceFile.NonCompilableSourceFile(sourceFileFromLibrary)
          )
        }.toMutableList()
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = expectedClassFilesToOpen)

        assertThat(actualResult).describedAs(psiElementTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }

    @ParameterizedTest
    @ArgumentsSource(ClassPsiElementTestVectors::class)
    fun `Given PSI elements from a class file in a library, When using findByPsiElement, Then get the correct class files to open`(
      psiElementTestVector: PsiElementTestVector
    ) {
      invokeReadActionAndWait {
        val classFileFromLibrary =
          myExecutionTestCase.getClassFileFromLibrary(psiElementTestVector.baseFqClassName.toRelativeFilePath("class"))
        val testPsiElement = psiElementTestVector.psiElementReference.find(PsiManager.getInstance(project).findFile(classFileFromLibrary)!!)
        val actualResult = classFilesFinderService.findByPsiElement(testPsiElement, testPsiElement.containingFile)

        val expectedClassFilesToOpen = psiElementTestVector.expectedFqClassNames.map { fqClassName ->
          ClassFile(
            file = myExecutionTestCase.getClassFileFromLibrary(fqClassName.toRelativeFilePath("class")),
            sourceFile = if (isSourceMirrorAvailableInLibraryClassFile(psiElementTestVector.relativeSourceFilePath)) {
              SourceFile.NonCompilableSourceFile(
                myExecutionTestCase.getSourceFileFromLibrary(psiElementTestVector.relativeSourceFilePath)
              )
            }
            else {
              null
            }
          )
        }.toMutableList()
        val expectedResult = ClassFilesFinderService.Result(classFilesToOpen = expectedClassFilesToOpen)

        assertThat(actualResult).describedAs(psiElementTestVector.sourceVirtualFile.name).isEqualTo(expectedResult)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  abstract class PsiReference {

    abstract fun find(psiFile: PsiFile): PsiElement

    protected fun findContainingClassNames(ktElement: KtElement): List<String> {
      val parentClassOrObject = ktElement.getParentOfType<KtClassOrObject>(true) ?: return emptyList()
      return findContainingClassNames(parentClassOrObject) +
              (parentClassOrObject.classIdIfNonLocal?.let { listOf(it.shortClassName.asString()) } ?: emptyList())
    }

    protected fun findContainingClassNames(psiElement: PsiElement): List<String> {
      val parentClass = psiElement.getParentOfType<PsiClass>(true) ?: return emptyList()
      return findContainingClassNames(parentClass) +
              (parentClass.classIdIfNonLocal?.let { listOf(it.shortClassName.asString()) } ?: emptyList())
    }

    protected fun visitFile(
      psiFile: PsiFile,
      visitKotlinElement: ((KtElement) -> Unit)? = null,
      visitJavaElement: ((PsiElement) -> Unit)? = null,
    ) {
      when (psiFile.fileType) {
        KotlinFileType.INSTANCE -> {
          assertThat(visitKotlinElement).isNotNull()
          psiFile.accept(object : KotlinRecursiveElementVisitor() {

            override fun visitKtElement(element: KtElement) {
              super.visitKtElement(element)
              visitKotlinElement!!(element)
            }
          })
        }

        JavaFileType.INSTANCE, JavaClassFileType.INSTANCE -> {
          assertThat(visitJavaElement).isNotNull()
          psiFile.accept(object : JavaRecursiveElementVisitor() {

            override fun visitElement(element: PsiElement) {
              super.visitElement(element)
              visitJavaElement!!(element)
            }
          })
        }

        else -> throw IllegalArgumentException("Unknown file type: $fileType")
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class PsiMethodReference(val containingClassNames: List<String>, val methodName: String) : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      visitFile(
        psiFile = psiFile,
        visitKotlinElement = { element ->
          println(element::class.simpleName + ": " + if (element is KtNamedFunction) element.name else (if (element is KtClass) element.name else ""))
          if (element is KtNamedFunction && element.name == methodName && containingClassNames == findContainingClassNames(element)) {
            assertThat(result).isNull()
            result = element
          }
        },
        visitJavaElement = { element ->
          if (element is PsiMethod && element.name == methodName && containingClassNames == findContainingClassNames(element)) {
            assertThat(result).isNull()
            result = element
          }
        }
      )

      assertThat(result).describedAs("Find method '$methodName' in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class PsiModuleExportsReference(val packageName: String) : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      visitFile(
        psiFile = psiFile,
        visitJavaElement = { element ->
          if (element is PsiJavaModule) {
            assertThat(result).isNull()
            result = element.exports.find { it.packageName == packageName }
          }
        }
      )

      assertThat(result).describedAs("Find module exports '$packageName' in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class PsiEnumValueReference(val valueName: String) : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      visitFile(
        psiFile = psiFile,
        visitKotlinElement = { element ->
          if (element is KtEnumEntry && element.name == valueName) {
            assertThat(result).isNull()
            result = element
          }
        },
        visitJavaElement = { element ->
          if (element is PsiEnumConstant && element.name == valueName) {
            assertThat(result).isNull()
            result = element
          }
        }
      )

      assertThat(result).describedAs("Find enum value '$valueName' in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class PsiBeginOfFileReference : PsiReference() {

    override fun find(psiFile: PsiFile): PsiElement {
      var result: PsiElement? = null

      visitFile(
        psiFile = psiFile,
        visitKotlinElement = { element ->
          if (result == null) {
            result = element
          }
        },
        visitJavaElement = { element ->
          if (result == null) {
            result = element
          }
        },
      )

      assertThat(result).describedAs("Find method first element in file $psiFile").isNotNull()
      return result!!
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  @Ignore
  internal class MyExecutionTestCase : ExecutionTestCase() {

    private val testProjectPath = Path.of("testProject").absolute()
    private val sourceDirs = listOf(
      testProjectPath.resolve(Path.of("src", "main", "java")),
      testProjectPath.resolve(Path.of("src", "main", "kotlin"))
    )

    internal lateinit var psiElementTestVectors: List<PsiElementTestVector>
    internal lateinit var fileTestVectors: List<FileTestVector>

    private var inSetUp = true
    private lateinit var sourcesJar: VirtualFile
    private lateinit var classesJar: VirtualFile

    init {
      name = this::class.simpleName
    }

    override fun initOutputChecker(): OutputChecker = OutputChecker({ testProjectPath.toString() }, { "out" })

    override fun getTestAppPath(): String = testProjectPath.toString()

    public override fun setUp() {
      // The `super.setUp()` will run `compileProject()` but the language to the
      // module is not set yet. As of IntelliJ 2023.3, the default language will
      // be Java 8, which can't handle certain language constructs, like Java
      // module descriptors.
      inSetUp = true
      super.setUp()
      IdeaTestUtil.setModuleLanguageLevel(module, projectLanguageLevel, testRootDisposable)
      inSetUp = false

      compileProject()

      addProjectAsLibrary()

      fileTestVectors = collectFileTestVectors()
      assertThat(fileTestVectors).isNotEmpty

      psiElementTestVectors = collectPsiElementTestVectors()
      assertThat(psiElementTestVectors).isNotEmpty
    }

    private fun addProjectAsLibrary() {

      val classesAJarPath = tempDir.newPath("classesA.jar", true)
      copyDirToJar(classesAJarPath, listOf(moduleOutputDir))
      classesJar = invokeReadActionAndWait { VfsUtil.findFileByIoFile(classesAJarPath.toFile(), true) }!!

      val sourcesAJarPath = tempDir.newPath("sourcesA.jar", true)
      copyDirToJar(sourcesAJarPath, sourceDirs)
      sourcesJar = invokeReadActionAndWait { VfsUtil.findFileByIoFile(sourcesAJarPath.toFile(), true) }!!

      // Create library with classes and sources
      val libraryWithClassesAndSources = PsiTestUtil.addProjectLibrary(
        module,
        "my-library",
        listOf(JarFileSystem.getInstance().getRootByLocal(classesJar)!!),
        listOf(JarFileSystem.getInstance().getRootByLocal(sourcesJar)!!)
      )
      Disposer.register(testRootDisposable) {
        runWriteAction { LibraryTablesRegistrar.getInstance().getLibraryTable(project).removeLibrary(libraryWithClassesAndSources) }
      }
    }

    fun getSourceFileFromLibrary(relativeSourceFilePath: Path): VirtualFile {
      val fileInSourceJar = JarFileSystem.getInstance().getJarRootForLocalFile(sourcesJar)?.findFile(relativeSourceFilePath.toString())
      assertThat(fileInSourceJar).describedAs(relativeSourceFilePath.toString()).isNotNull()
      assertThat(LibraryUtil.findLibraryEntry(fileInSourceJar!!, project)).isNotNull()
      return fileInSourceJar
    }

    fun getClassFileFromLibrary(relativeClassFilePath: Path): VirtualFile {
      val fileInClassesJar = JarFileSystem.getInstance().getJarRootForLocalFile(classesJar)?.findFile(relativeClassFilePath.toString())
      assertThat(fileInClassesJar).describedAs(relativeClassFilePath.toString()).isNotNull()
      assertThat(LibraryUtil.findLibraryEntry(fileInClassesJar!!, project)).isNotNull()
      return fileInClassesJar
    }

    public override fun tearDown() {
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
      JarOutputStream(Files.newOutputStream(jarFilePath, StandardOpenOption.APPEND, StandardOpenOption.CREATE)).use { jar ->
        dirs.forEach { dir ->
          Files.walk(dir).filter { it.isRegularFile() }.forEach { path ->
            val entry = dir.relativize(path).toString()
            val jarEntry = JarEntry(entry)
            jar.putNextEntry(jarEntry)
            if (Files.isRegularFile(path)) Files.copy(path, jar)
            jar.closeEntry()
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

        val testVectorMarkerResult = Regex("// FileTestVector\\{(.*)}").find(psiElement.text) ?: return@walkSourceDirs
        val testVectorMarker = testVectorMarkerResult.parseTestVectorMarker()
        val containingFqClassNames = testVectorMarker["containingFqClassNames"]!!.split("|").filter { it.isNotBlank() }
        val baseFqClassNames = testVectorMarker["baseFqClassNames"]!!.split("|").filter { it.isNotBlank() }
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
            emptyKotlinFile = emptyKotlinFile
          )
        )
      }

      return fileTestVector
    }

    private fun collectPsiElementTestVectors(): List<PsiElementTestVector> {
      val psiElementTestVectors = mutableListOf<PsiElementTestVector>()

      walkSourceDirs { psiElement, sourceFile, sourceVirtualFile, sourcePsiFile, sourceLanguage ->
        if (psiElement !is PsiComment) {
          return@walkSourceDirs
        }

        val testVectorMarkerResult = Regex("// PsiElementTestVector\\{(.*)}").find(psiElement.text) ?: return@walkSourceDirs
        val testVectorMarker = testVectorMarkerResult.parseTestVectorMarker()
        val baseFqClassName = testVectorMarker["baseFqClassName"]!!
        val expectedFqClassNames = testVectorMarker["expectedFqClassNames"]!!.split("|").filter { it.isNotBlank() }
        val sourceFileOnly = testVectorMarker["sourceFileOnly"]?.toBoolean() ?: false
        val reference = testVectorMarker["reference"]!!
        val psiReference = when {
          reference.startsWith("METHOD|") -> {
            val referencePath = reference.substringAfter("|")
            val (fqClassName, methodName) = if ('#' in referencePath) referencePath.split("#") else listOf("", referencePath)
            val containingClassNames =
              if ('$' in fqClassName) fqClassName.split('$') else (if (fqClassName.isNotBlank()) listOf(fqClassName) else emptyList())
            PsiMethodReference(containingClassNames, methodName)
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
            sourceFileOnly = sourceFileOnly
          )
        )
      }

      return psiElementTestVectors
    }

    private fun MatchResult.parseTestVectorMarker(): Map<String, String> {
      val testVectorMarker = this.groupValues[1]
        .replace("[", "").replace("]", "")
        .split(",")
        .associate {
          val (key, value) = it.split(":")
          key.trim() to value.trim()
        }
      return testVectorMarker
    }

    private fun walkSourceDirs(visitPsiElement: (PsiElement, Path, VirtualFile, PsiFile, Language) -> Unit) {
      sourceDirs.forEach { sourceDir ->
        Files.walkFileTree(sourceDir, object : SimpleFileVisitor<Path>() {
          private val consumableFileExtensions = listOf("java", "kt", "kts")

          override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (!consumableFileExtensions.contains(file.extension)) {
              return FileVisitResult.CONTINUE
            }

            val sourceVirtualFile = VfsUtil.findFileByIoFile(file.toFile(), true)
            assertThat(sourceVirtualFile).describedAs("Virtual file for $file").isNotNull()
            val sourceLanguage: Language = when {
              sourceVirtualFile!!.isJavaFileType() -> JavaLanguage.INSTANCE
              sourceVirtualFile.isKotlinFileType() -> KotlinLanguage.INSTANCE
              else -> throw IllegalArgumentException("Can't determine language of file: $this")
            }
            invokeReadActionAndWait {
              VirtualFileManager.getInstance().syncRefresh()
              var sourcePsiFile: PsiFile? = PsiManager.getInstance(project).findViewProvider(sourceVirtualFile)!!.getPsi(sourceLanguage)
              if (sourcePsiFile == null || sourceVirtualFile.name.contains("$")) {
                val containingVirtualFile = sourceVirtualFile.parent?.findFile("${sourceVirtualFile.name.substringBefore('$')}.class")
                if (containingVirtualFile != null) {
                  sourcePsiFile = PsiManager.getInstance(project).findFile(containingVirtualFile)
                }
              }
              assertThat(sourcePsiFile).describedAs("Find PSI file for: $file").isNotNull()
              sourcePsiFile!!.accept(object : KotlinRecursiveElementVisitor() {

                override fun visitElement(element: PsiElement) {
                  super.visitElement(element)
                  visitPsiElement(element, file, sourceVirtualFile, sourcePsiFile, sourceLanguage)
                }
              })
            }

            return FileVisitResult.CONTINUE
          }
        })
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class FileTestVector(
    val title: String,
    val sourceVirtualFile: VirtualFile,
    val sourcePsiFile: PsiFile,
    val relativeSourceFilePath: Path,
    val containingFqClassNames: List<String>,
    val baseFqClassNames: List<String>,
    val sourceFileOnly: Boolean,
    val emptyKotlinFile: Boolean
  ) {

    override fun toString(): String = "${sourceVirtualFile.name}: $title"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class PsiElementTestVector(
    val title: String,
    val sourceVirtualFile: VirtualFile,
    val relativeSourceFilePath: Path,
    val sourcePsiFile: PsiFile,
    val sourceLanguage: Language,
    val psiElementReference: PsiReference,
    val baseFqClassName: String,
    val expectedFqClassNames: List<String>,
    val sourceFileOnly: Boolean
  ) {

    override fun toString(): String = "${sourceVirtualFile.name}: $title"
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class SourcePsiElementTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
      myExecutionTestCase.psiElementTestVectors.map { Arguments.of(it) }.stream()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ClassPsiElementTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
      myExecutionTestCase.psiElementTestVectors.filter { !it.sourceFileOnly }.map { Arguments.of(it) }.stream()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class SourceFileTestVectors : ArgumentsProvider {

    override fun provideArguments(context: ExtensionContext): Stream<Arguments> =
      myExecutionTestCase.fileTestVectors.map { Arguments.of(it) }.stream()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

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

  // -- Companion Object -------------------------------------------------------------------------------------------- //

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

    fun <T> invokeReadActionAndWait(action: () -> T): T? {
      var result: T? = null
      ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runReadAction {
          result = action()
        }
      }
      return result
    }
  }
}
package dev.turingcomplete.intellijbytecodeplugin

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatform4TestCase
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.random.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert

abstract class ClassFileConsumerTestCase(private val classFilePaths: List<String>) :
  LightPlatform4TestCase() {
  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {
    private const val LIMIT_CLASSES = 800
    private const val BATCH_SIZE = 40

    fun testData(): List<Array<Any>> =
      mutableListOf<Array<Any>>().apply {
        addLibraryClasses("kotlin-stdlib")
        addLibraryClasses("groovy-")
        addLibraryClasses("commons-lang3")
      }

    private fun MutableList<Array<Any>>.addLibraryClasses(libraryFileNamePrefix: String) {
      val oldSize = this.size
      findInClassPath(libraryFileNamePrefix).forEach { library ->
        readArchiveEntriesPaths(library.toFile())
          .shuffled(Random(0))
          .take(LIMIT_CLASSES)
          .chunked(BATCH_SIZE)
          .forEachIndexed { index, classFilePaths ->
            add(arrayOf("${library.fileName} batch ${index + 1}", classFilePaths))
          }
      }
      assertThat((this.size - oldSize) * BATCH_SIZE)
        .describedAs(
          "Library with filename prefix '$libraryFileNamePrefix' should add at least 100 files"
        )
        .isGreaterThanOrEqualTo(100)
    }

    private fun findInClassPath(prefix: String) =
      System.getProperty("java.class.path")
        .split(System.getProperty("path.separator"))
        .asSequence()
        .map { Path.of(it) }
        .filter { it.fileName.toString().startsWith(prefix) }
        .toList()

    private fun readArchiveEntriesPaths(archiveFile: File): List<String> {
      val entriesPaths = mutableListOf<String>()

      ZipFile(archiveFile).use { zipFile ->
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
          val zipEntry = entries.nextElement()
          if (zipEntry.name.endsWith(".class")) {
            entriesPaths.add("jar://$archiveFile!/${zipEntry.name}")
          }
        }
      }

      return entriesPaths
    }
  }

  // -- Properties
  // -------------------------------------------------------------------------------------------------- //

  // -- Initialization
  // ----------------------------------------------------------------------------------------------
  // //
  // -- Exposed Methods
  // ---------------------------------------------------------------------------------------------
  // //

  override fun setUp() {
    super.setUp()

    WriteAction.runAndWait<Throwable> {
      FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, "jmod")
    }
  }

  protected fun consumeClassFiles(consumer: (VirtualFile) -> Unit) {
    classFilePaths.forEach { classFilePath ->
      val virtualFile = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
      Assert.assertNotNull("File $classFilePath not found", virtualFile)
      try {
        consumer(virtualFile as VirtualFile)
      } catch (cause: Throwable) {
        throw AssertionError("Failed to consume class file: $classFilePath", cause)
      }
    }
  }

  // -- Private Methods
  // ---------------------------------------------------------------------------------------------
  // //
  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //
}

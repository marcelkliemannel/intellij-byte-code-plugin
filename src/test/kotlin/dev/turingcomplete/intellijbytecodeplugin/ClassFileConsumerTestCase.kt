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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert

abstract class ClassFileConsumerTestCase(private val classFilePath: String) :
  LightPlatform4TestCase() {
  // -- Companion Object
  // -------------------------------------------------------------------------------------------- //

  companion object {
    private const val LIMIT_CLASSES = 800

    fun testData(): List<Array<String>> =
      mutableListOf<Array<String>>().apply {
        addLibraryClasses("kotlin-stdlib")
        addLibraryClasses("groovy-")
        addLibraryClasses("commons-lang3")
      }

    private fun MutableList<Array<String>>.addLibraryClasses(libraryFileNamePrefix: String) {
      val oldSize = this.size
      findInClassPath(libraryFileNamePrefix).forEach { kotlinStdLib ->
        this.addAll(readArchiveEntriesPaths(kotlinStdLib.toFile()).shuffled().take(LIMIT_CLASSES))
      }
      assertThat(this.size - oldSize)
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

    private fun readArchiveEntriesPaths(archiveFile: File): List<Array<String>> {
      val entriesPaths = mutableListOf<Array<String>>()

      ZipFile(archiveFile).use { zipFile ->
        val entries = zipFile.entries()
        while (entries.hasMoreElements()) {
          val zipEntry = entries.nextElement()
          if (zipEntry.name.endsWith(".class")) {
            entriesPaths.add(arrayOf(zipEntry.name, "jar://$archiveFile!/${zipEntry.name}"))
          }
        }
      }

      return entriesPaths
    }
  }

  // -- Properties
  // -------------------------------------------------------------------------------------------------- //

  protected lateinit var classFileAsVirtualFile: VirtualFile

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

    val virtualFile0 = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
    Assert.assertNotNull("File $classFilePath not found", virtualFile0)
    classFileAsVirtualFile = virtualFile0 as VirtualFile
  }

  // -- Private Methods
  // ---------------------------------------------------------------------------------------------
  // //
  // -- Inner Type
  // -------------------------------------------------------------------------------------------------- //
}

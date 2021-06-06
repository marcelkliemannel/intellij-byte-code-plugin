package dev.turingcomplete.intellijbytecodeplugin

import com.intellij.ide.highlighter.ArchiveFileType
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightPlatform4TestCase
import org.junit.Assert
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

abstract class ClassFileConsumerTestCase(val classFilePath: String) : LightPlatform4TestCase() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val LIMIT_CLASSES = 800

    fun testData(): List<Array<String>> {
      val testParameters = mutableListOf<Array<String>>()

      // Test parsing of Kotlin classes
      findKotlinSdtLibInClassPath()
              .forEach { kotlinStdLib -> testParameters.addAll(readArchiveEntriesPaths(kotlinStdLib.toFile()).shuffled().take(LIMIT_CLASSES)) }
      Assert.assertTrue(testParameters.size > 100)

      // Test parsing of Groovy classes
      findGroovyAllInClassPath()
              .forEach { groovyAll -> testParameters.addAll(readArchiveEntriesPaths(groovyAll.toFile()).shuffled().take(LIMIT_CLASSES)) }
      Assert.assertTrue(testParameters.size > 200)

      // Test parsing of java.base classes
      val javaBaseJmodPath = Path.of(System.getProperty("java.home")).resolve(Path.of("jmods", "java.base.jmod"))
      testParameters.addAll(readArchiveEntriesPaths(javaBaseJmodPath.toFile()).shuffled().take(LIMIT_CLASSES))
      Assert.assertTrue(testParameters.size > 300)

      return testParameters
    }

    private fun findKotlinSdtLibInClassPath(): List<Path> {
      return getClassPath()
              .filter { it.fileName.toString().startsWith("kotlin-stdlib") }
              .toList()
    }

    private fun findGroovyAllInClassPath(): List<Path> {
      return getClassPath()
              .filter { it.fileName.toString().startsWith("groovy-all-") }
              .toList()
    }

    private fun getClassPath(): List<Path> {
      return System.getProperty("java.class.path").split(System.getProperty("path.separator"))
              .asSequence()
              .map { Path.of(it) }
              .toList()
    }

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

  // -- Properties -------------------------------------------------------------------------------------------------- //

  protected lateinit var virtualFile: VirtualFile

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun setUp() {
    super.setUp()

    WriteAction.runAndWait<Throwable> {
      FileTypeManager.getInstance().associateExtension(ArchiveFileType.INSTANCE, "jmod")
    }

    val virtualFile0 = VirtualFileManager.getInstance().findFileByUrl(classFilePath)
    Assert.assertNotNull("File $classFilePath not found", virtualFile0)
    virtualFile = virtualFile0 as VirtualFile
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
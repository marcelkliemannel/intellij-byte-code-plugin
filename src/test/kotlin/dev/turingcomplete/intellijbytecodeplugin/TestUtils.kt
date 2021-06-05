package dev.turingcomplete.intellijbytecodeplugin

import org.junit.Assert
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipFile

object TestUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  // todo move to abstract super class

  private const val LIMIT_CLASSES = 800

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun findKotlinSdtLibInClassPath(): List<Path> {
    return getClassPath()
            .filter { it.fileName.toString().startsWith("kotlin-stdlib") }
            .toList()
  }

  fun findGroovyAllInClassPath(): List<Path> {
    return getClassPath()
            .filter { it.fileName.toString().startsWith("groovy-all-") }
            .toList()
  }

  fun data(): List<Array<String>> {
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //

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

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
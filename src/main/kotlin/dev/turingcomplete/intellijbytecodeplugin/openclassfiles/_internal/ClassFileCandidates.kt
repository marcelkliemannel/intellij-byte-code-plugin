package dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import dev.turingcomplete.intellijbytecodeplugin.common._internal.joinAsNaturalLanguage
import org.jsoup.internal.StringUtil.StringJoiner
import java.nio.file.Path
import kotlin.io.path.relativeToOrNull

sealed class ClassFileCandidates private constructor(val primaryPath: Path, private val fallbackPaths: List<Path> = emptyList()) {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun allPaths() = listOf(primaryPath) + fallbackPaths

  fun formatNotFoundError(postfix: String = "cannot be found.", project: Project? = null): String =
    StringJoiner(" ").apply {
      add("Class file")
      add("'${formatPath(primaryPath, project)}'")
      if (fallbackPaths.isNotEmpty()) {
        add("or possible fallback class file${if (fallbackPaths.size >= 2) "s" else ""}")
        add(fallbackPaths.joinAsNaturalLanguage { "'${formatPath(it, project)}'" })
      }
      add(postfix)
    }.complete()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ClassFileCandidates) return false

    if (primaryPath != other.primaryPath) return false
    if (fallbackPaths != other.fallbackPaths) return false

    return true
  }

  override fun hashCode(): Int {
    var result = primaryPath.hashCode()
    result = 31 * result + fallbackPaths.hashCode()
    return result
  }

  override fun toString(): String {
    return "ClassFileCandidates(primaryPath=$primaryPath, fallbackPaths=$fallbackPaths)"
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun formatPath(path: Path, project: Project?): String {
    val projectDir = project?.guessProjectDir()?.toNioPath()
    return if (projectDir != null) {
      path.relativeToOrNull(projectDir)?.toString() ?: path.toString()
    }
    else {
      path.toString()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class RelativeClassFileCandidates(primaryPath: Path, fallbackPaths: List<Path>)
    : ClassFileCandidates(primaryPath, fallbackPaths)

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class AbsoluteClassFileCandidates(primaryPath: Path, fallbackPaths: List<Path>)
    : ClassFileCandidates(primaryPath, fallbackPaths)

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    fun fromRelativePaths(vararg paths: Path): RelativeClassFileCandidates {
      assert(paths.all { !it.isAbsolute })
      assert(paths.isNotEmpty())

      val fallbackPaths = if (paths.size > 1) paths.sliceArray(1 until paths.size) else emptyArray()
      return RelativeClassFileCandidates(primaryPath = paths.first(), fallbackPaths = fallbackPaths.toList())
    }

    fun fromAbsolutePaths(vararg paths: Path): AbsoluteClassFileCandidates {
      assert(paths.all { it.isAbsolute })
      assert(paths.isNotEmpty())

      val fallbackPaths = if (paths.size > 1) paths.sliceArray(1 until paths.size) else emptyArray()
      return AbsoluteClassFileCandidates(primaryPath = paths.first(), fallbackPaths = fallbackPaths.toList())
    }
  }
}
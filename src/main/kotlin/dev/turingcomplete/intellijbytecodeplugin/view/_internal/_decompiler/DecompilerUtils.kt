package dev.turingcomplete.intellijbytecodeplugin.view._internal._decompiler

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers

object DecompilerUtils {
  // -- Properties ---------------------------------------------------------- //

  private const val IDEA_DECOMPILER_FQ_CLASS_NAME = "org.jetbrains.java.decompiler.IdeaDecompiler"

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun decompile(classFile: VirtualFile, project: Project): String? {
    val decompilers = findDecompilersForFile(classFile)

    val outermostClassFileName = "${classFile.name.substringBefore('$')}.class"
    val isNestedClassFile = classFile.name != outermostClassFileName

    val psiManager = PsiManager.getInstance(project)
    var decompiledSourceCode: String? = decompileClassFile(classFile, decompilers, psiManager)
    if (decompiledSourceCode == null && isNestedClassFile) {
      // Some decompilers have trouble decompiling standalone nested class
      // files. What might help is to decompile the outermost class. Some
      // decompilers will then include the nested class in that file.
      val outermostClassFile = classFile.parent.findFile(outermostClassFileName)
      if (outermostClassFile != null) {
        decompiledSourceCode = decompileClassFile(outermostClassFile, decompilers, psiManager)
      }
    }

    return decompiledSourceCode
  }

  fun findDecompilersForFile(classFile: VirtualFile): List<ClassFileDecompilers.Decompiler> {
    val decompilers =
      ClassFileDecompilers.getInstance().EP_NAME.extensions
        .filter { it.accepts(classFile) }
        // Prefer the `IdeaDecompiler` decompiler, as it produces better results.
        .sortedBy { it.javaClass.name != IDEA_DECOMPILER_FQ_CLASS_NAME }
        .toList()
    return decompilers
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun decompileClassFile(
    classFile: VirtualFile,
    decompilers: List<ClassFileDecompilers.Decompiler>,
    psiManager: PsiManager,
  ): String? {
    decompilers.forEach { decompiler ->
      if (decompiler is ClassFileDecompilers.Full) {
        val createFileViewProvider = decompiler.createFileViewProvider(classFile, psiManager, true)
        val decompiledText =
          createFileViewProvider.getPsi(createFileViewProvider.baseLanguage)?.text
        if (!decompiledText.isNullOrBlank()) {
          return decompiledText
        }
      } else if (decompiler is ClassFileDecompilers.Light) {
        val decompiledText = decompiler.getText(classFile).toString()
        if (decompiledText.isNotBlank()) {
          return decompiledText
        }
      }
    }
    return null
  }

  // -- Inner Type ---------------------------------------------------------- //
}

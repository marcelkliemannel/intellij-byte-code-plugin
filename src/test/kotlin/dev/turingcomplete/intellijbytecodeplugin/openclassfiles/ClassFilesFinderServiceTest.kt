package dev.turingcomplete.intellijbytecodeplugin.openclassfiles

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import dev.turingcomplete.intellijbytecodeplugin.ClassFileConsumerTestCase
import dev.turingcomplete.intellijbytecodeplugin.openclassfiles._internal.ClassFilesFinderService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(org.junit.runners.Parameterized::class)
class ClassFilesFinderServiceTest(
  @Suppress("UNUSED_PARAMETER") testName: String,
  classFilePath: String
) : ClassFileConsumerTestCase(classFilePath) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @org.junit.runners.Parameterized.Parameters(name = "{0}")
    @JvmStatic
    fun data(): List<Array<String>> = testData()
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var psiJavaFile: PsiJavaFile? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun setUp() {
    super.setUp()

    psiJavaFile = ReadAction.compute<PsiJavaFile, Throwable> {
      PsiManager.getInstance(project).findFile(classFileAsVirtualFile) as PsiJavaFile?
    }
  }

  @Test
  fun testOpenFile() {
    val actualClassFilesReadyToOpen = mutableListOf<VirtualFile>()
    ClassFilesFinderService(project)
      .findByVirtualFiles(listOf(classFileAsVirtualFile), { actualClassFilesReadyToOpen.add(it.file) })
    Assert.assertEquals(1, actualClassFilesReadyToOpen.size)
    Assert.assertEquals(classFileAsVirtualFile, actualClassFilesReadyToOpen[0])
  }

  @Test
  fun testOpenPsiFile() {
    if (psiJavaFile == null) {
      return
    }

    val actualClassFilesReadyToOpen = mutableListOf<VirtualFile>()
    ClassFilesFinderService(project)
      .findByPsiFiles(listOf(psiJavaFile!!), { actualClassFilesReadyToOpen.add(it.file) })
    Assert.assertEquals(1, actualClassFilesReadyToOpen.size)
    Assert.assertEquals(classFileAsVirtualFile, actualClassFilesReadyToOpen[0])
  }

  @Test
  fun testOpenPsiElements() {
    if (psiJavaFile == null) {
      return
    }
    val psiFile0 = psiJavaFile as PsiJavaFile

    val psiElements = mutableListOf<PsiElement>()
    psiFile0.classes.forEach { psiClass -> psiElements.addAll(getPsiElementsOfPsiClass(psiClass)) }
    psiFile0.packageStatement?.let { psiElements.add(it) }
    psiFile0.moduleDeclaration?.let {
      psiElements.add(it)
      psiElements.addAll(it.children)
    }
    doTestConsumePsiElements(psiElements, psiFile0.name)
  }

  /**
   * When `virtualFile` is an inner class, the class file to open should be the
   * correct separate inner class file and not the outer one. In the PSI context,
   * the containing file of an inner class is the outer class, which is not
   * the right one to open.
   */
  @Test
  fun testInnerClassPsiElements() {
    if (!classFileAsVirtualFile.name.contains("$") || psiJavaFile == null) {
      return
    }

    psiJavaFile!!.classes.forEach { psiClass ->
      doTestConsumePsiElements(
        getPsiElementsOfPsiClass(psiClass),
        "${collectJvmName(psiClass)}.class"
      )
    }
  }

  private fun collectJvmName(psiClass: PsiClass?): String {
    if (psiClass == null || psiClass.name == null) {
      return ""
    }

    val parentName = collectJvmName(PsiTreeUtil.getParentOfType(psiClass, PsiClass::class.java))
    return "${if (parentName.isNotBlank()) "$parentName$" else ""}${psiClass.name}"
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun getPsiElementsOfPsiClass(psiClass: PsiClass): List<PsiElement> {
    val psiElements = mutableListOf<PsiElement>()
    psiElements.add(psiClass)
    psiClass.allFields.forEach { psiFile -> psiElements.add(psiFile) }
    psiClass.allMethods.forEach { psiMethod ->
      psiElements.add(psiMethod)
      psiElements.addAll(psiMethod.children)
    }
    return psiElements
  }

  private fun doTestConsumePsiElements(psiElements: List<PsiElement>, expectedClassFileName: String) {
    Assert.assertTrue("Expected at least 1 PSI element.", psiElements.isNotEmpty())

    var filesOpened = 0
    ClassFilesFinderService(project)
      .findByPsiElements(psiElements, {
        filesOpened++
        assertThat(expectedClassFileName)
          .describedAs("File ${it.file.path} should have expected class file name")
          .isEqualTo(it.file.name)
      })
    Assert.assertEquals(psiElements.size, filesOpened)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}
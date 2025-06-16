package com.trendyol.transmission.visualizer.analysis

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.trendyol.transmission.visualizer.detection.DetectedTransmission
import com.trendyol.transmission.visualizer.isTestFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile

class TransmissionElementFinder(private val project: Project) {

    fun findTransmissionElement(transmission: DetectedTransmission): PsiElement? {
        return ReadAction.compute<PsiElement?, Exception> {
            runCatching {
                val scope = GlobalSearchScope.projectScope(project)
                val allKtFiles = FilenameIndex.getAllFilesByExt(project, "kt", scope)

                for (virtualFile in allKtFiles) {
                    val psiFile =
                        com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile)
                    if (psiFile is KtFile) {
                        if (isTestFile(psiFile.name, virtualFile.path)) {
                            continue
                        }

                        val foundElement = findInFile(psiFile, transmission)
                        if (foundElement != null) {
                            return@compute foundElement
                        }
                    }
                }
                null
            }.getOrNull()
        }
    }

    private fun findInFile(ktFile: KtFile, transmission: DetectedTransmission): PsiElement? {
        val allClassOrObjects = PsiTreeUtil.findChildrenOfType(ktFile, KtClassOrObject::class.java)

        for (element in allClassOrObjects) {
            if (element.name == transmission.name) {
                val lineNumber = getLineNumber(element)
                if (lineNumber == transmission.lineNumber || transmission.lineNumber == -1) {
                    return element
                }
            }
        }

        return null
    }

    private fun getLineNumber(element: PsiElement): Int {
        return runCatching {
            val document = element.containingFile?.viewProvider?.document
            if (document != null) {
                document.getLineNumber(element.textOffset) + 1
            } else {
                -1
            }
        }.getOrElse { -1 }
    }
}

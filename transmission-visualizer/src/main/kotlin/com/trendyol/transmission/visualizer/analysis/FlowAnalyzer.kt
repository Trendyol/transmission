package com.trendyol.transmission.visualizer.analysis

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.trendyol.transmission.visualizer.detection.DetectedTransmission
import com.trendyol.transmission.visualizer.findContainingClassName
import com.trendyol.transmission.visualizer.isTestFile

class FlowAnalyzer(private val project: Project) {

    private val elementFinder = TransmissionElementFinder(project)
    private val contextAnalyzer = ContextAnalyzer()

    fun analyzeTransmissionFlow(transmission: DetectedTransmission): List<TransmissionUsage> {
        return ReadAction.compute<List<TransmissionUsage>, Exception> {
            runCatching {
                val usages = mutableListOf<TransmissionUsage>()
                val transmissionElement = elementFinder.findTransmissionElement(transmission)

                if (transmissionElement != null) {
                    val searchScope = GlobalSearchScope.projectScope(project)
                    val references =
                        ReferencesSearch.search(transmissionElement, searchScope).findAll()

                    references.forEach { reference ->
                        val element = reference.element
                        val containingFile = element.containingFile

                        if (!isTestFile(containingFile.name, containingFile.virtualFile?.path)) {
                            val usage = createTransmissionUsage(element)
                            if (usage != null) {
                                usages.add(usage)
                            }
                        }
                    }
                }

                usages.sortedBy { it.context.ordinal }
            }.getOrElse {
                emptyList()
            }
        }
    }

    private fun createTransmissionUsage(element: PsiElement): TransmissionUsage? {
        try {
            val containingFile = element.containingFile
            val document = containingFile.viewProvider.document
            val lineNumber = if (document != null) {
                document.getLineNumber(element.textOffset) + 1
            } else {
                -1
            }

            val context = contextAnalyzer.determineUsageContext(element)
            val className = element.findContainingClassName()

            return TransmissionUsage(
                element = element,
                fileName = containingFile.name,
                lineNumber = lineNumber,
                context = context,
                className = className
            )
        } catch (_: Exception) {
            return null
        }
    }
}

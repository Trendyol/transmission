package com.trendyol.transmission.visualizer.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.trendyol.transmission.visualizer.detection.TransmissionDetector
import com.trendyol.transmission.visualizer.toolwindow.TransmissionFlowAction
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration

class TransmissionLineMarkerProvider : LineMarkerProvider {

    private val transmissionDetector = TransmissionDetector()

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = runCatching {
        val ktElement = when (element) {
            is KtClass -> element
            is KtObjectDeclaration -> element
            else -> return null
        }
        val elementName = ktElement.name ?: return null
        val elementLineNumber = ktElement.getLineNumber() + 1
        val detectionResult = transmissionDetector.detectTransmissions(ktElement.containingKtFile)

        if (!ktElement.isValid) {
            return null
        }

        val matchingTransmission = detectionResult.find { signal ->
            signal.name == elementName && signal.lineNumber == elementLineNumber
        } ?: return null

        return LineMarkerInfo(
            ktElement,
            ktElement.textRange,
            AllIcons.Nodes.Plugin,
            { "Transmission Visualizer: Click to see flow" },
            TransmissionFlowAction(matchingTransmission, matchingTransmission.type.typeName),
            GutterIconRenderer.Alignment.CENTER,
            { "Transmission ${matchingTransmission.type.typeName}: ${matchingTransmission.name}" }
        )
    }.getOrNull()
}

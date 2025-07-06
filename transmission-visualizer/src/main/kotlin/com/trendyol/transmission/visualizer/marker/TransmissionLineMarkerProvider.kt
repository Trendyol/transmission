package com.trendyol.transmission.visualizer.marker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import com.trendyol.transmission.visualizer.detection.DetectedTransmission
import com.trendyol.transmission.visualizer.detection.TransmissionDetector
import com.trendyol.transmission.visualizer.toolwindow.TransmissionFlowAction
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class TransmissionLineMarkerProvider : LineMarkerProvider {

    private val transmissionDetector = TransmissionDetector()
    private val fileDetectionCache = ContainerUtil.createConcurrentWeakKeySoftValueMap<KtFile, CachedDetectionResult>()

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = runCatching {
        if (!isTransmissionNameIdentifier(element)) {
            return null
        }

        val parentElement = element.parent as? KtNamedDeclaration ?: return null
        
        if (!parentElement.isValid) {
            return null
        }

        val parentName = parentElement.name ?: return null
        val parentLineNumber = parentElement.getLineNumber() + 1
        val containingFile = parentElement.containingFile as? KtFile ?: return null
        val detectionResult = getOrCacheDetectionResult(containingFile)
        
        val matchingTransmission = detectionResult.find { signal ->
            signal.name == parentName && signal.lineNumber == parentLineNumber
        } ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            AllIcons.Nodes.Plugin,
            { "Transmission Visualizer: Click to see flow" },
            TransmissionFlowAction(matchingTransmission, matchingTransmission.type.typeName),
            GutterIconRenderer.Alignment.CENTER,
            { "Transmission ${matchingTransmission.type.typeName}: ${matchingTransmission.name}" }
        )
    }.getOrNull()

    private fun isTransmissionNameIdentifier(element: PsiElement): Boolean {
        val parent = element.parent
        return when (parent) {
            is KtClass -> parent.nameIdentifier == element
            is KtObjectDeclaration -> parent.nameIdentifier == element
            else -> false
        }
    }

    /**
     * Get cached detection result or compute and cache it.
     * This avoids expensive repeated file scanning.
     */
    private fun getOrCacheDetectionResult(ktFile: KtFile): List<DetectedTransmission> {
        val currentModificationStamp = ktFile.modificationStamp
        
        val cachedResult = fileDetectionCache[ktFile]
        if (cachedResult != null && cachedResult.modificationStamp == currentModificationStamp) {
            return cachedResult.detectionResult
        }

        val detectionResult = transmissionDetector.detectTransmissions(ktFile)
        fileDetectionCache[ktFile] = CachedDetectionResult(detectionResult, currentModificationStamp)
        
        return detectionResult
    }

    private data class CachedDetectionResult(
        val detectionResult: List<DetectedTransmission>,
        val modificationStamp: Long
    )
}

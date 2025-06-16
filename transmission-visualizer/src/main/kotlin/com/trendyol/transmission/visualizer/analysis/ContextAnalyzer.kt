package com.trendyol.transmission.visualizer.analysis

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.trendyol.transmission.visualizer.parents
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtUserType

class ContextAnalyzer {

    fun determineUsageContext(element: PsiElement): UsageContext {
        return when {
            isRoutingContext(element) -> UsageContext.ROUTING
            isPublicationContext(element) -> UsageContext.PUBLICATION
            isProcessingContext(element) -> UsageContext.PROCESSING
            isConsumptionContext(element) -> UsageContext.CONSUMPTION
            isCreationContext(element) -> UsageContext.CREATION
            else -> UsageContext.PROCESSING
        }
    }

    private fun isRoutingContext(element: PsiElement): Boolean {
        val callExpression = findContainingCallExpression(element) ?: return false
        val calleeExpression = callExpression.calleeExpression
        return when (calleeExpression) {
            is KtDotQualifiedExpression -> {
                val selectorExpression = calleeExpression.selectorExpression
                val receiverText = calleeExpression.receiverExpression.text

                (receiverText.contains("router") || receiverText.contains("Router")) &&
                        selectorExpression?.text == "process"
            }

            is KtNameReferenceExpression -> {
                calleeExpression.getReferencedName() == "process"
            }

            else -> false
        }
    }

    private fun isPublicationContext(element: PsiElement): Boolean {
        val callExpression = findContainingCallExpression(element) ?: return false
        val calleeExpression = callExpression.calleeExpression
        return when (calleeExpression) {
            is KtNameReferenceExpression -> {
                val referencedName = calleeExpression.getReferencedName()
                referencedName == "publish" || referencedName == "send"
            }

            is KtDotQualifiedExpression -> {
                val selectorExpression = calleeExpression.selectorExpression
                selectorExpression?.text == "publish" || selectorExpression?.text == "send"
            }

            else -> false
        }
    }

    private fun isProcessingContext(element: PsiElement): Boolean {
        val lambdaExpression = element.parents.filterIsInstance<KtLambdaExpression>().firstOrNull()
        val callExpression = lambdaExpression?.parent?.parent as? KtCallExpression ?: return false
        val calleeText = callExpression.calleeExpression?.text

        return calleeText == "onSignal" || calleeText == "onEffect"
    }

    private fun isConsumptionContext(element: PsiElement): Boolean {
        val callExpression =
            element.parents.filterIsInstance<KtCallExpression>().firstOrNull() ?: return false
        val calleeExpression = callExpression.calleeExpression
        return when (calleeExpression) {
            is KtDotQualifiedExpression -> {
                val receiverText = calleeExpression.receiverExpression.text
                val selectorText = calleeExpression.selectorExpression?.text

                (receiverText.contains("stream") || receiverText.contains("Stream")) &&
                        (selectorText == "collect" || selectorText == "collectLatest" ||
                                selectorText == "asState" || selectorText == "toList")
            }

            is KtNameReferenceExpression -> {
                val referencedName = calleeExpression.getReferencedName()
                referencedName == "collect" || referencedName == "collectLatest"
            }

            else -> false
        }
    }

    private fun isCreationContext(element: PsiElement): Boolean {
        if (element is KtClass || element is KtObjectDeclaration) {
            return isElementImplementingTransmission(element)
        }

        val callExpression = element.parents.filterIsInstance<KtCallExpression>().firstOrNull()
        val objectLiteral =
            element.parents.filterIsInstance<KtObjectLiteralExpression>().firstOrNull()
        val containingClass = element.parents.filterIsInstance<KtClass>().firstOrNull()
        val containingObject = element.parents.filterIsInstance<KtObjectDeclaration>().firstOrNull()

        return when {
            callExpression != null && isDirectInstantiation(element, callExpression) -> true
            objectLiteral != null -> true
            containingClass != null && isElementImplementingTransmission(containingClass) -> true
            containingObject != null && isElementImplementingTransmission(containingObject) -> true
            else -> false
        }
    }

    private fun isElementImplementingTransmission(element: PsiElement): Boolean {
        return when (element) {
            is KtClass -> {
                val superTypeList = element.getSuperTypeList()
                superTypeList?.entries?.any { superTypeEntry ->
                    val typeReference = superTypeEntry.typeAsUserType
                    typeReference?.let { userType ->
                        isTransmissionType(userType)
                    } ?: false
                } ?: false
            }

            is KtObjectDeclaration -> {
                val superTypeList = element.getSuperTypeList()
                superTypeList?.entries?.any { superTypeEntry ->
                    val typeReference = superTypeEntry.typeAsUserType
                    typeReference?.let { userType ->
                        isTransmissionType(userType)
                    } ?: false
                } ?: false
            }

            else -> false
        }
    }

    private fun findContainingCallExpression(element: PsiElement): KtCallExpression? {
        var current = element.parent
        while (current != null) {
            if (current is KtCallExpression) {
                val arguments = current.valueArguments
                for (arg in arguments) {
                    if (PsiTreeUtil.isAncestor(arg, element, false)) {
                        return current
                    }
                }
            }
            current = current.parent
        }
        return null
    }

    private fun isDirectInstantiation(
        element: PsiElement,
        callExpression: KtCallExpression
    ): Boolean {
        val calleeExpression = callExpression.calleeExpression
        return when (calleeExpression) {
            is KtNameReferenceExpression -> {
                calleeExpression.getReferencedName() == element.text
            }

            is KtDotQualifiedExpression -> {
                calleeExpression.selectorExpression?.text == element.text
            }

            else -> false
        }
    }

    private fun isTransmissionType(userType: KtUserType): Boolean {
        val typeText = userType.text
        return typeText.contains("Transmission.Signal") ||
                typeText.contains("Transmission.Effect") ||
                typeText.contains("Transmission.Data") ||
                typeText.endsWith("Signal") ||
                typeText.endsWith("Effect") ||
                typeText.endsWith("Data")
    }
}

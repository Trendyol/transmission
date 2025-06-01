package com.trendyol.transmission.visualizer.analysis

import com.intellij.psi.PsiElement

data class TransmissionUsage(
    val element: PsiElement,
    val fileName: String,
    val lineNumber: Int,
    val context: UsageContext,
    val className: String?,
)

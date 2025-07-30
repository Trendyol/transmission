package com.trendyol.transmission.visualizer

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtObjectDeclaration

val PsiElement.parents: Sequence<PsiElement>
    get() = generateSequence(this.parent) { it.parent }

fun isTestFile(fileName: String, filePath: String?): Boolean {
    val lowerFileName = fileName.lowercase()
    val lowerPath = filePath?.lowercase() ?: ""

    return lowerFileName.contains("test") ||
            lowerPath.contains("/test/") ||
            lowerPath.contains("/androidtest/") ||
            lowerPath.contains("/unittest/") ||
            lowerFileName.endsWith("test.kt") ||
            lowerFileName.endsWith("tests.kt")
}

fun PsiElement.findContainingClassName(): String? {
    return this.parents.filterIsInstance<KtClass>().firstOrNull()?.name
        ?: this.parents.filterIsInstance<KtObjectDeclaration>().firstOrNull()?.name
}

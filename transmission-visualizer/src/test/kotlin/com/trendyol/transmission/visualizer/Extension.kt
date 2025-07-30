package com.trendyol.transmission.visualizer

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration

fun Project.createKtFile(fileName: String = "test.kt", kotlinCode: String): KtFile {
    return PsiFileFactory.getInstance(this).createFileFromText(
        fileName,
        KotlinFileType.INSTANCE,
        kotlinCode
    ) as KtFile
}

fun PsiElement.findCallByName(calleeName: String): KtCallExpression? {
    val callExpressions = PsiTreeUtil.findChildrenOfType(this, KtCallExpression::class.java).toList()
    return callExpressions.find { it.calleeExpression?.text == calleeName }
}

fun PsiElement.findCallContaining(calleeText: String): KtCallExpression? {
    val callExpressions = PsiTreeUtil.findChildrenOfType(this, KtCallExpression::class.java).toList()

    return callExpressions.find { it.calleeExpression?.text?.contains(calleeText) == true }
}

fun PsiElement.findNameReference(referencedName: String): KtNameReferenceExpression? {
    val nameReferences = PsiTreeUtil.findChildrenOfType(this, KtNameReferenceExpression::class.java).toList()
    return nameReferences.find { it.getReferencedName() == referencedName }
}

fun PsiElement.findNameReference(referencedName: String, condition: (KtNameReferenceExpression) -> Boolean): KtNameReferenceExpression? {
    val nameReferences = PsiTreeUtil.findChildrenOfType(this, KtNameReferenceExpression::class.java).toList()
    return nameReferences.find { it.getReferencedName() == referencedName && condition(it) }
}

fun PsiElement.findClass(className: String): KtClass? {
    val classes = PsiTreeUtil.findChildrenOfType(this, KtClass::class.java).toList()
    return classes.find { it.name == className }
}

fun PsiElement.findObject(objectName: String): KtObjectDeclaration? {
    val objects = PsiTreeUtil.findChildrenOfType(this, KtObjectDeclaration::class.java).toList()
    return objects.find { it.name == objectName }
}

fun KtNameReferenceExpression.isConstructorCall(): Boolean {
    return parent?.text?.startsWith("${getReferencedName()}(") == true
}

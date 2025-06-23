package com.trendyol.transmission.visualizer.detection

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtUserType

class TransmissionDetector {

    fun detectTransmissions(psiFile: PsiFile): List<DetectedTransmission> {
        if (psiFile !is KtFile) {
            return emptyList()
        }

        val detectedTransmissionList = mutableListOf<DetectedTransmission>()
        val signalInterfaces = findTransmissionInterfaces(psiFile, TransmissionType.SIGNAL)
        val effectInterfaces = findTransmissionInterfaces(psiFile, TransmissionType.EFFECT)
        val classDeclarations = PsiTreeUtil.findChildrenOfType(psiFile, KtClassOrObject::class.java)

        classDeclarations.forEach { classDeclaration ->
            val className = classDeclaration.name ?: ""
            val isSealedInterface = classDeclaration is KtClass &&
                    classDeclaration.isInterface() &&
                    classDeclaration.hasModifier(KtTokens.SEALED_KEYWORD)

            val lineNumber = classDeclaration.getLineNumber() + 1
            val isSignalClass = isTransmissionClass(
                classDeclaration = classDeclaration,
                transmissionInterfaces = signalInterfaces,
                ktFile = psiFile,
                type = TransmissionType.SIGNAL,
            )
            val isEffectClass = isTransmissionClass(
                classDeclaration = classDeclaration,
                transmissionInterfaces = effectInterfaces,
                ktFile = psiFile,
                type = TransmissionType.EFFECT,
            )

            if (isSignalClass && isSealedInterface.not()) {
                detectedTransmissionList.add(
                    DetectedTransmission(
                        name = className,
                        lineNumber = lineNumber,
                        type = TransmissionType.SIGNAL,
                    )
                )
            }

            if (isEffectClass && isSealedInterface.not()) {
                detectedTransmissionList.add(
                    DetectedTransmission(
                        name = className,
                        lineNumber = lineNumber,
                        type = TransmissionType.EFFECT,
                    )
                )
            }
        }

        return detectedTransmissionList
    }

    private fun findTransmissionInterfaces(ktFile: KtFile, type: TransmissionType): Set<String> {
        val allInterfaces = PsiTreeUtil.findChildrenOfType(ktFile, KtClass::class.java)
            .filter { it.isInterface() && it.hasModifier(KtTokens.SEALED_KEYWORD) }

        val dependencyGraph = buildInterfaceDependencyGraph(allInterfaces)
        
        val directInterfaces = allInterfaces
            .filter { isDirectTransmissionInterface(it, ktFile, type.typeName) }
            .mapNotNull { it.name }
            .toSet()
        
        return findAllTransmissionInterfacesTopological(dependencyGraph, directInterfaces)
    }

    private fun buildInterfaceDependencyGraph(interfaces: List<KtClass>): Map<String, Set<String>> {
        val graph = mutableMapOf<String, MutableSet<String>>()
        
        interfaces.forEach { interfaceElement ->
            val name = interfaceElement.name ?: return@forEach
            graph[name] = mutableSetOf()
            
            interfaceElement.getSuperTypeList()?.entries?.forEach { superType ->
                val superName = superType.typeAsUserType?.referencedName
                if (superName != null && superName != "Transmission") {
                    graph[name]?.add(superName)
                }
            }
        }
        
        return graph
    }

    private fun findAllTransmissionInterfacesTopological(
        graph: Map<String, Set<String>>,
        directInterfaces: Set<String>
    ): Set<String> {
        val result = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        
        fun depthFirstSearch(node: String) {
            if (node in visited) return
            visited.add(node)
            
            if (node in directInterfaces || graph[node]?.any { it in result } == true) {
                result.add(node)
                
                graph.entries.forEach { (dependent, dependencies) ->
                    if (node in dependencies && dependent !in visited) {
                        depthFirstSearch(dependent)
                    }
                }
            }
        }
        
        directInterfaces.forEach { depthFirstSearch(it) }
        
        graph.keys.forEach { interfaceName ->
            if (interfaceName !in visited) {
                depthFirstSearch(interfaceName)
            }
        }
        
        return result
    }

    private fun isTransmissionClass(
        classDeclaration: KtClassOrObject,
        transmissionInterfaces: Set<String>,
        ktFile: KtFile,
        type: TransmissionType
    ): Boolean {
        if (directlyImplementsTransmission(classDeclaration, ktFile, type.typeName)) {
            return true
        }

        if (implementsTransmissionSealedInterface(classDeclaration, transmissionInterfaces)) {
            return true
        }

        return false
    }

    private fun directlyImplementsTransmission(
        classDeclaration: KtClassOrObject,
        ktFile: KtFile,
        typeName: String
    ): Boolean {
        val superTypeList = classDeclaration.getSuperTypeList() ?: return false

        for (superTypeEntry in superTypeList.entries) {
            val typeReference = superTypeEntry.typeAsUserType ?: continue

            if (isTransmissionTypeReference(typeReference, ktFile, typeName)) {
                return true
            }
        }

        return false
    }

    private fun implementsTransmissionSealedInterface(
        classDeclaration: KtClassOrObject,
        transmissionInterfaces: Set<String>
    ): Boolean {
        val superTypeList = classDeclaration.getSuperTypeList() ?: return false

        for (superTypeEntry in superTypeList.entries) {
            val typeReference = superTypeEntry.typeAsUserType ?: continue
            val referencedName = typeReference.referencedName ?: continue

            if (transmissionInterfaces.contains(referencedName)) {
                return true
            }
        }

        return false
    }

    private fun isTransmissionTypeReference(
        typeReference: KtUserType,
        ktFile: KtFile,
        typeName: String
    ): Boolean {
        val referencedName = typeReference.referencedName

        return when {
            referencedName == typeName && isTransmissionQualified(typeReference) -> true
            referencedName == typeName && hasTransmissionImport(ktFile, typeName) -> true
            typeReference.text.contains("Transmission.$typeName") -> true
            else -> false
        }
    }

    private fun isTransmissionQualified(typeReference: KtUserType): Boolean {
        val qualifier = typeReference.qualifier
        return qualifier?.referencedName == "Transmission"
    }

    private fun isDirectTransmissionInterface(
        ktClass: KtClass,
        ktFile: KtFile,
        typeName: String
    ): Boolean {
        val superTypeList = ktClass.getSuperTypeList() ?: return false

        for (superTypeEntry in superTypeList.entries) {
            val typeReference = superTypeEntry.typeAsUserType ?: continue

            if (isTransmissionTypeReference(typeReference, ktFile, typeName)) {
                return true
            }
        }

        return false
    }

    private fun hasTransmissionImport(ktFile: KtFile, typeName: String): Boolean {
        return ktFile.importDirectives.any { importDirective ->
            val importPath = importDirective.importedFqName?.asString()
            importPath == "com.trendyol.transmission.Transmission.$typeName" ||
                    importPath == "com.trendyol.transmission.Transmission"
        }
    }
} 
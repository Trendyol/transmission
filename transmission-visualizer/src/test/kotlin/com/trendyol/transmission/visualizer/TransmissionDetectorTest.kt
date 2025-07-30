package com.trendyol.transmission.visualizer

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.trendyol.transmission.visualizer.detection.TransmissionDetector
import com.trendyol.transmission.visualizer.detection.TransmissionType

class TransmissionDetectorTest : BasePlatformTestCase() {
    
    private lateinit var detector: TransmissionDetector

    override fun setUp() {
        super.setUp()
        detector = TransmissionDetector()
    }

    //region SIGNAL DETECTION TESTS

    fun testSealedInterfaceWithSingleSignal() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface InputSignal : Transmission.Signal {
                data class InputUpdate(val value: String) : InputSignal
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(0, effects.size)
        assertEquals("InputUpdate", signals[0].name)
    }
    
    fun testDirectSignalClassImplementation() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            data class InputUpdate(val value: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(0, effects.size)
        assertEquals("InputUpdate", signals[0].name)
    }
    
    fun testDirectImportSignalInterface() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission.Signal

            data class InputUpdate(val value: String) : Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(0, effects.size)
        assertEquals("InputUpdate", signals[0].name)
    }
    
    fun testObjectImplementingSignal() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            object RefreshSignal : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(0, effects.size)
        assertEquals("RefreshSignal", signals[0].name)
    }

    fun testSealedInterfaceWithMultipleSignals() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface UserSignal : Transmission.Signal {
                data class UserUpdate(val id: String, val name: String) : UserSignal
                object UserRefresh : UserSignal
                data class UserDelete(val id: String) : UserSignal
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(3, signals.size)
        assertEquals(0, effects.size)
        
        // Check signal names
        assertEquals("UserUpdate", signals[0].name)
        assertEquals("UserRefresh", signals[1].name)
        assertEquals("UserDelete", signals[2].name)
    }

    //endregion

    //region EFFECT DETECTION TESTS

    fun testSealedInterfaceWithSingleEffect() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface InputEffect : Transmission.Effect {
                data class InputUpdate(val value: String) : InputEffect
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(1, effects.size)
        assertEquals("InputUpdate", effects[0].name)
    }
    
    fun testDirectEffectClassImplementation() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            data class InputUpdate(val value: String) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(1, effects.size)
        assertEquals("InputUpdate", effects[0].name)
    }
    
    fun testDirectImportEffectInterface() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission.Effect

            data class InputUpdate(val value: String) : Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(1, effects.size)
        assertEquals("InputUpdate", effects[0].name)
    }
    
    fun testObjectImplementingEffect() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            object RefreshEffect : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(1, effects.size)
        assertEquals("RefreshEffect", effects[0].name)
    }

    fun testSealedInterfaceWithMultipleEffects() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface UserEffect : Transmission.Effect {
                data class UserUpdate(val id: String, val name: String) : UserEffect
                object UserRefresh : UserEffect
                data class UserDelete(val id: String) : UserEffect
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(3, effects.size)
        
        // Check effect names
        assertEquals("UserUpdate", effects[0].name)
        assertEquals("UserRefresh", effects[1].name)
        assertEquals("UserDelete", effects[2].name)
    }

    //endregion

    //region MIXED TRANSMISSION TESTS

    fun testFileWithBothSignalsAndEffects() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface UserSignal : Transmission.Signal {
                data class UserCreated(val id: String) : UserSignal
                object UserRefresh : UserSignal
            }

            sealed interface UserEffect : Transmission.Effect {
                data class ShowNotification(val message: String) : UserEffect
                object NavigateToProfile : UserEffect
            }

            data class DirectSignal(val data: String) : Transmission.Signal
            data class DirectEffect(val data: String) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(3, signals.size)
        assertEquals(3, effects.size)
        
        // Check signal names
        assertEquals("UserCreated", signals[0].name)
        assertEquals("UserRefresh", signals[1].name)
        assertEquals("DirectSignal", signals[2].name)
        
        // Check effect names
        assertEquals("ShowNotification", effects[0].name)
        assertEquals("NavigateToProfile", effects[1].name)
        assertEquals("DirectEffect", effects[2].name)
    }

    fun testComplexTransmissionPatterns() {
        // Given
        val kotlinCode = """
            package com.example.transmissions
            
            import com.trendyol.transmission.Transmission
            import com.trendyol.transmission.Transmission.Signal
            import com.trendyol.transmission.Transmission.Effect
            
            // Pattern 1: Sealed interfaces with multiple members
            sealed interface UserSignal : Transmission.Signal {
                data class UserCreated(val id: String, val name: String, val email: String) : UserSignal
                data class UserUpdated(val id: String, val changes: Map<String, Any>) : UserSignal
                data class UserDeleted(val id: String) : UserSignal
                object UserRefreshRequested : UserSignal
            }
            
            sealed interface NotificationEffect : Transmission.Effect {
                data class ShowToast(val message: String, val duration: Long) : NotificationEffect
                data class ShowDialog(val title: String, val message: String) : NotificationEffect
                object DismissAll : NotificationEffect
            }
            
            // Pattern 2: Direct implementations with complex parameters
            data class OrderProcessedSignal(
                val orderId: String,
                val customerId: String,
                val items: List<String>,
                val totalAmount: Double,
                val timestamp: Long
            ) : Transmission.Signal
            
            data class NavigateToPageEffect(
                val route: String,
                val params: Map<String, Any>,
                val clearStack: Boolean
            ) : Transmission.Effect
            
            // Pattern 3: Direct import style
            data class PaymentCompletedSignal(val transactionId: String) : Signal
            data class UpdateUIEffect(val data: String) : Effect
            
            // Pattern 4: Objects for global events
            object AppStartSignal : Transmission.Signal
            object ClearCacheEffect : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(7, signals.size)
        assertEquals(6, effects.size)
        
        // Verify some key signals
        assertTrue(signals.any { it.name == "UserCreated" })
        assertTrue(signals.any { it.name == "UserUpdated" })
        assertTrue(signals.any { it.name == "OrderProcessedSignal" })
        assertTrue(signals.any { it.name == "PaymentCompletedSignal" })
        assertTrue(signals.any { it.name == "AppStartSignal" })
        
        // Verify some key effects
        assertTrue(effects.any { it.name == "ShowToast" })
        assertTrue(effects.any { it.name == "ShowDialog" })
        assertTrue(effects.any { it.name == "NavigateToPageEffect" })
        assertTrue(effects.any { it.name == "UpdateUIEffect" })
        assertTrue(effects.any { it.name == "ClearCacheEffect" })
    }

    fun testComplexPattern1SealedInterfaces() {
        // Given - Pattern 1: Sealed interfaces with multiple members
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            sealed interface UserSignal : Transmission.Signal {
                data class UserCreated(val id: String, val name: String, val email: String) : UserSignal
                data class UserUpdated(val id: String, val changes: Map<String, Any>) : UserSignal
                data class UserDeleted(val id: String) : UserSignal
                object UserRefreshRequested : UserSignal
            }
            
            sealed interface NotificationEffect : Transmission.Effect {
                data class ShowToast(val message: String, val duration: Long) : NotificationEffect
                data class ShowDialog(val title: String, val message: String) : NotificationEffect
                object DismissAll : NotificationEffect
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(4, signals.size)
        assertEquals(3, effects.size)
    }

    fun testComplexPattern2DirectImplementations() {
        // Given - Pattern 2: Direct implementations with complex parameters
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            data class OrderProcessedSignal(
                val orderId: String,
                val customerId: String,
                val items: List<String>,
                val totalAmount: Double,
                val timestamp: Long
            ) : Transmission.Signal
            
            data class NavigateToPageEffect(
                val route: String,
                val params: Map<String, Any>,
                val clearStack: Boolean
            ) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(1, effects.size)
    }

    fun testComplexPattern3DirectImportStyle() {
        // Given - Pattern 3: Direct import style
        val kotlinCode = """
            import com.trendyol.transmission.Transmission.Signal
            import com.trendyol.transmission.Transmission.Effect
            
            data class PaymentCompletedSignal(val transactionId: String) : Signal
            data class UpdateUIEffect(val data: String) : Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(1, effects.size)
    }

    fun testComplexPattern4Objects() {
        // Given - Pattern 4: Objects for global events
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            object AppStartSignal : Transmission.Signal
            object ClearCacheEffect : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(1, signals.size)
        assertEquals(1, effects.size)
    }

    fun testNestedSealedInterfaces() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface CartSignal : Transmission.Signal
            sealed interface ItemSignal : CartSignal {
                data class AddItem(val productId: String) : ItemSignal
                data class RemoveItem(val productId: String) : ItemSignal
            }

            sealed interface PaymentEffect : Transmission.Effect
            sealed interface CreditCardEffect : PaymentEffect {
                data class ProcessPayment(val amount: Double) : CreditCardEffect
                object ValidateCard : CreditCardEffect
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(2, signals.size)
        assertEquals(2, effects.size)
        
        // Check nested signal implementations
        assertEquals("AddItem", signals[0].name)
        assertEquals("RemoveItem", signals[1].name)
        
        // Check nested effect implementations
        assertEquals("ProcessPayment", effects[0].name)
        assertEquals("ValidateCard", effects[1].name)
    }

    //endregion

    //region EDGE CASES

    fun testEmptyFile() {
        // Given
        val kotlinCode = ""
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        
        // Then
        assertEquals(0, result.size)
    }

    fun testFileWithoutTransmissions() {
        // Given
        val kotlinCode = """
            package com.example.test
            
            data class RegularClass(val value: String)
            
            interface RegularInterface {
                fun doSomething()
            }
            
            object RegularObject
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        val signals = result.filter { it.type == TransmissionType.SIGNAL }
        val effects = result.filter { it.type == TransmissionType.EFFECT }
        
        // Then
        assertEquals(0, signals.size)
        assertEquals(0, effects.size)
    }

    fun testOnlySealedInterfacesWithoutImplementations() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission

            sealed interface UserSignal : Transmission.Signal
            sealed interface UserEffect : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val result = detector.detectTransmissions(psiFile)
        
        // Then
        assertEquals(0, result.size)
    }

    //endregion
}

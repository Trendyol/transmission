package com.trendyol.transmission.visualizer

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.trendyol.transmission.visualizer.analysis.ContextAnalyzer
import com.trendyol.transmission.visualizer.analysis.UsageContext

class ContextAnalyzerTest : BasePlatformTestCase() {

    private lateinit var analyzer: ContextAnalyzer

    override fun setUp() {
        super.setUp()
        analyzer = ContextAnalyzer()
    }

    //region USAGE CONTEXT DETECTION

    fun testRoutingContextWithDotQualifiedExpression() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyTransformer {
                fun process() {
                    router.process(MySignal("test"))
                }
            }
            
            data class MySignal(val data: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val processCall = psiFile.findCallContaining("process")
        val signalReference = processCall?.findNameReference("MySignal")
        val context = analyzer.determineUsageContext(signalReference!!)

        // Then
        assertNotNull(signalReference)
        assertEquals(UsageContext.ROUTING, context)
    }

    fun testRoutingContextWithSimpleProcessCall() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyTransformer {
                fun handleSignal() {
                    process(MySignal("test"))
                }
            }
            
            data class MySignal(val data: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val processCall = psiFile.findCallByName("process")
        val signalReference = processCall?.findNameReference("MySignal")
        val context = analyzer.determineUsageContext(signalReference!!)

        // Then
        assertNotNull(signalReference)
        assertEquals(UsageContext.ROUTING, context)
    }

    fun testPublicationContextWithPublishCall() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyViewModel {
                fun triggerEffect() {
                    publish(ShowToastEffect("Hello"))
                }
            }
            
            data class ShowToastEffect(val message: String) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val publishCall = psiFile.findCallByName("publish")
        val effectReference = publishCall?.findNameReference("ShowToastEffect")
        val context = analyzer.determineUsageContext(effectReference!!)
        
        // Then
        assertNotNull(effectReference)
        assertEquals(UsageContext.PUBLICATION, context)
    }

    fun testPublicationContextWithSendCall() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyViewModel {
                fun triggerEffect() {
                    send(NavigateEffect("/home"))
                }
            }
            
            data class NavigateEffect(val route: String) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val sendCall = psiFile.findCallByName("send")
        val effectReference = sendCall?.findNameReference("NavigateEffect")
        val context = analyzer.determineUsageContext(effectReference!!)
        
        // Then
        assertNotNull(effectReference)
        assertEquals(UsageContext.PUBLICATION, context)
    }

    fun testPublicationContextWithDotQualifiedSend() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyViewModel {
                fun triggerEffect() {
                    effectPublisher.send(NavigateEffect("/profile"))
                }
            }
            
            data class NavigateEffect(val route: String) : Transmission.Effect
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val sendCall = psiFile.findCallContaining("send")
        val effectReference = sendCall?.findNameReference("NavigateEffect")
        val context = analyzer.determineUsageContext(effectReference!!)
        
        // Then
        assertNotNull(effectReference)
        assertEquals(UsageContext.PUBLICATION, context)
    }

    fun testProcessingContextWithOnSignal() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyTransformer {
                fun setup() {
                    onSignal { signal: UserSignal ->
                        when (signal) {
                            is UserSignal.UserCreated -> handleCreated(signal)
                        }
                    }
                }
            }
            
            sealed interface UserSignal : Transmission.Signal {
                data class UserCreated(val id: String) : UserSignal
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userCreatedReference = psiFile.findNameReference("UserCreated")
        
        // Then
        assertNotNull(userCreatedReference)
        val context = analyzer.determineUsageContext(userCreatedReference!!)
        assertEquals(UsageContext.PROCESSING, context)
    }

    fun testProcessingContextWithOnEffect() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyEffectHandler {
                fun setup() {
                    onEffect { effect: NavigationEffect ->
                        when (effect) {
                            is NavigationEffect.Navigate -> navigate(effect.route)
                        }
                    }
                }
            }
            
            sealed interface NavigationEffect : Transmission.Effect {
                data class Navigate(val route: String) : NavigationEffect
            }
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val navigateReference = psiFile.findNameReference("Navigate")
        val context = analyzer.determineUsageContext(navigateReference!!)
        
        // Then
        assertNotNull(navigateReference)
        assertEquals(UsageContext.PROCESSING, context)
    }

    fun testConsumptionContextWithStreamCollect() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyObserver {
                fun observeData() {
                    dataStream.collect { data ->
                        when (data) {
                            is UserData -> displayUser(data)
                        }
                    }
                }
            }
            
            data class UserData(val name: String) : Transmission.Data
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userDataReference = psiFile.findNameReference("UserData")
        val context = analyzer.determineUsageContext(userDataReference!!)
        
        // Then
        assertNotNull(userDataReference)
        assertEquals(UsageContext.CONSUMPTION, context)
    }

    fun testConsumptionContextWithStreamCollectLatest() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyObserver {
                fun observeData() {
                    userStream.collectLatest { user: UserData ->
                        updateUI(user)
                    }
                }
            }
            
            data class UserData(val name: String) : Transmission.Data
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userDataReference = psiFile.findNameReference("UserData")
        val context = analyzer.determineUsageContext(userDataReference!!)
        
        // Then
        assertNotNull(userDataReference)
        assertEquals(UsageContext.CONSUMPTION, context)
    }

    fun testConsumptionContextWithAsState() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class MyComposable {
                fun render() {
                    val userState = userStream.asState(UserData("default"))
                    UserView(userState.value)
                }
            }
            
            data class UserData(val name: String) : Transmission.Data
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userDataReference = psiFile.findNameReference("UserData") { it.isConstructorCall() }
        val context = analyzer.determineUsageContext(userDataReference!!)
        
        // Then
        assertNotNull(userDataReference)
        assertEquals(UsageContext.CREATION, context)
    }

    fun testCreationContextWithClassDefinition() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            data class UserSignal(val id: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userSignalClass = psiFile.findClass("UserSignal")
        
        // Then
        assertNotNull(userSignalClass)
        val context = analyzer.determineUsageContext(userSignalClass!!)
        assertEquals(UsageContext.CREATION, context)
    }

    fun testCreationContextWithObjectDefinition() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            object RefreshSignal : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val refreshSignalObject = psiFile.findObject("RefreshSignal")
        val context = analyzer.determineUsageContext(refreshSignalObject!!)
        
        // Then
        assertNotNull(refreshSignalObject)
        assertEquals(UsageContext.CREATION, context)
    }

    fun testDefaultProcessingContextForUnknownUsage() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class RandomClass {
                fun randomMethod() {
                    val signal = UserSignal("test")
                    someUnknownMethod(signal)
                }
            }
            
            data class UserSignal(val data: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        // Look for the signal reference used in the someUnknownMethod call, not the constructor call
        val userSignalReference = psiFile.findNameReference("signal")
        val context = analyzer.determineUsageContext(userSignalReference!!)
        
        // Then
        assertNotNull(userSignalReference)
        assertEquals(UsageContext.PROCESSING, context)
    }

    //endregion

    //region CONTAINING CLASS NAME

    fun testFindContainingClassNameFromClass() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class UserViewModel {
                fun createUser() {
                    val signal = UserSignal("123")
                    publish(signal)
                }
            }
            
            data class UserSignal(val id: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userSignalReference = psiFile.findNameReference("UserSignal") { it.isConstructorCall() }
        val className = userSignalReference!!.findContainingClassName()
        
        // Then
        assertNotNull(userSignalReference)
        assertEquals("UserViewModel", className)
    }

    fun testFindContainingClassNameFromObject() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            object UserRepository {
                fun saveUser() {
                    val signal = UserSavedSignal("123")
                    publish(signal)
                }
            }
            
            data class UserSavedSignal(val id: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val signalReference = psiFile.findNameReference("UserSavedSignal") { it.isConstructorCall() }
        val className = signalReference!!.findContainingClassName()
        
        // Then
        assertNotNull(signalReference)
        assertEquals("UserRepository", className)
    }

    fun testFindContainingClassNameReturnsNullWhenNotInClass() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            fun topLevelFunction() {
                val signal = UserSignal("123")
                publish(signal)
            }
            
            data class UserSignal(val id: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userSignalReference = psiFile.findNameReference("UserSignal") { it.isConstructorCall() }
        val className = userSignalReference!!.findContainingClassName()
        
        // Then
        assertNotNull(userSignalReference)
        assertNull(className)
    }

    fun testFindContainingClassNameFromNestedClass() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class OuterClass {
                class InnerViewModel {
                    fun processSignal() {
                        val signal = ProcessSignal("data")
                        handle(signal)
                    }
                }
            }
            
            data class ProcessSignal(val data: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val signalReference = psiFile.findNameReference("ProcessSignal") { it.isConstructorCall() }
        val className = signalReference!!.findContainingClassName()
        
        // Then
        assertNotNull(signalReference)
        assertEquals("InnerViewModel", className)
    }

    //endregion

    //region EDGE CASES

    fun testComplexTransmissionUsageScenario() {
        // Given
        val kotlinCode = """
            import com.trendyol.transmission.Transmission
            
            class UserTransformer {
                fun setup() {
                    onSignal { signal: UserSignal ->
                        when (signal) {
                            is UserSignal.UserCreated -> {
                                val effect = UserEffect.ShowWelcome(signal.name)
                                publish(effect)
                            }
                            is UserSignal.UserDeleted -> {
                                router.process(CleanupSignal(signal.id))
                            }
                        }
                    }
                    
                    userStream.collect { user ->
                        updateUserDisplay(user)
                    }
                }
            }
            
            sealed interface UserSignal : Transmission.Signal {
                data class UserCreated(val id: String, val name: String) : UserSignal
                data class UserDeleted(val id: String) : UserSignal
            }
            
            sealed interface UserEffect : Transmission.Effect {
                data class ShowWelcome(val name: String) : UserEffect
            }
            
            data class CleanupSignal(val userId: String) : Transmission.Signal
        """.trimIndent()
        
        // When
        val psiFile = project.createKtFile(kotlinCode = kotlinCode)
        val userCreatedInWhen = psiFile.findNameReference("UserCreated")
        val showWelcomeInPublish = psiFile.findNameReference("ShowWelcome") { it.isConstructorCall() }
        val cleanupInRouter = psiFile.findNameReference("CleanupSignal") { it.isConstructorCall() }
        
        // Then
        assertNotNull(userCreatedInWhen)
        assertNotNull(showWelcomeInPublish) 
        assertNotNull(cleanupInRouter)
        
        assertEquals(UsageContext.PROCESSING, analyzer.determineUsageContext(userCreatedInWhen!!))
        assertEquals(UsageContext.PROCESSING, analyzer.determineUsageContext(showWelcomeInPublish!!))
        assertEquals(UsageContext.ROUTING, analyzer.determineUsageContext(cleanupInRouter!!))
        
        assertEquals("UserTransformer", userCreatedInWhen.findContainingClassName())
        assertEquals("UserTransformer", showWelcomeInPublish.findContainingClassName())
        assertEquals("UserTransformer", cleanupInRouter.findContainingClassName())
    }

    //endregion
}

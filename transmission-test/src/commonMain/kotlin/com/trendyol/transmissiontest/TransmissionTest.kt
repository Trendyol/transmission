package com.trendyol.transmissiontest

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.router.streamData
import com.trendyol.transmission.router.streamEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmissiontest.TransmissionTest.TestResult
import com.trendyol.transmissiontest.checkpoint.CheckpointTransformer
import com.trendyol.transmissiontest.checkpoint.CheckpointWithArgs
import com.trendyol.transmissiontest.checkpoint.CheckpointWithArgsTransformer
import com.trendyol.transmissiontest.checkpoint.DefaultCheckPoint
import com.trendyol.transmissiontest.computation.ComputationTransformer
import com.trendyol.transmissiontest.computation.ComputationWithArgsTransformer
import com.trendyol.transmissiontest.data.DataTransformer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.jvm.JvmName

/**
 * Comprehensive testing framework for Transmission components with a fluent DSL.
 *
 * TransmissionTest provides a sophisticated testing framework specifically designed for testing
 * [Transformer] components in isolation or with controlled dependencies. It supports mocking
 * data holders, computations, executions, and checkpoint validation while providing a clean
 * fluent API for test setup and assertions.
 *
 * ## Key Features:
 * - **Transformer Isolation**: Test transformers in isolation with mocked dependencies
 * - **Mock Support**: Mock data holders, computations, and executions from other transformers
 * - **Checkpoint Testing**: Support for testing experimental checkpoint-based flow control
 * - **Stream Capture**: Automatic capture of data and effect streams for assertions
 * - **Coroutine Testing**: Built-in integration with kotlinx-coroutines-test
 * - **Initial Processing**: Support for processing transmissions before the main test
 * - **Type-Safe Assertions**: Type-safe methods for asserting on specific data/effect types
 *
 * ## Basic Usage:
 *
 * ### Testing Signal Processing:
 * ```kotlin
 * @Test
 * fun testUserLogin() = runTest {
 *     val transformer = UserTransformer()
 *
 *     transformer.test()
 *         .testSignal(UserSignal.Login("username", "password")) {
 *             // Assert that login data was emitted
 *             val userData = lastData<UserData.LoggedIn>()
 *             assertNotNull(userData)
 *             assertEquals("username", userData.user.name)
 *         }
 * }
 * ```
 *
 * ### Testing with Mocked Dependencies:
 * ```kotlin
 * @Test
 * fun testUserProfile() = runTest {
 *     val transformer = ProfileTransformer()
 *
 *     transformer.test()
 *         .dataHolder(UserRepository.userContract) {
 *             UserData.LoggedIn(User("John", "john@example.com"))
 *         }
 *         .computation(ProfileService.profileContract) {
 *             UserProfile("John", 25, "Engineer")
 *         }
 *         .testSignal(ProfileSignal.LoadProfile) {
 *             val profileData = lastData<ProfileData.Loaded>()
 *             assertNotNull(profileData)
 *             assertEquals("John", profileData.profile.name)
 *         }
 * }
 * ```
 *
 * ### Testing Effect Processing:
 * ```kotlin
 * @Test
 * fun testCacheInvalidation() = runTest {
 *     val transformer = CacheTransformer()
 *
 *     transformer.test()
 *         .testEffect(CacheEffect.Invalidate("user_data")) {
 *             // Assert that cache was cleared
 *             val clearEffect = lastEffect<CacheEffect.Cleared>()
 *             assertNotNull(clearEffect)
 *         }
 * }
 * ```
 *
 * ### Testing with Initial Processing:
 * ```kotlin
 * @Test
 * fun testWithInitialState() = runTest {
 *     val transformer = DataTransformer()
 *
 *     transformer.test()
 *         .withInitialProcessing(
 *             UserSignal.Login("user", "pass"),
 *             DataSignal.Initialize
 *         )
 *         .testSignal(DataSignal.Refresh) {
 *             // Test behavior after initial processing
 *             assertTrue(dataStream.size >= 2)
 *         }
 * }
 * ```
 *
 * @param transformer The transformer to test
 * @param dispatcher The test dispatcher for controlling coroutine execution
 *
 * @see Transformer.test for creating test instances
 * @see TestResult for assertion methods
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransmissionTest
private constructor(
    private val transformer: Transformer,
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    private var router: TransmissionRouter? = null
    private val mockTransformers: MutableList<Transformer> = mutableListOf()
    private val initialTransmissions: MutableList<Transmission> = mutableListOf()
    private val orderedCheckpoints: MutableList<Contract.Checkpoint> = mutableListOf()

    /**
     * Adds a mock data holder that provides data for the specified contract during testing.
     *
     * This method creates a mock transformer that provides data when other transformers
     * query the specified data holder contract. The data is provided by the lambda function
     * which is called each time the contract is queried.
     *
     * @param D The type of data provided by the contract
     * @param contract The data holder contract to mock
     * @param data Lambda function that provides the data when queried
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .dataHolder(UserRepository.currentUserContract) {
     *         UserData.LoggedIn(User("test_user", "test@example.com"))
     *     }
     *     .testSignal(ProfileSignal.Load) { /* assertions */ }
     * ```
     */
    fun <D : Transmission.Data?> dataHolder(
        contract: Contract.DataHolder<D>,
        data: () -> D
    ): TransmissionTest {
        mockTransformers += DataTransformer(contract, data)
        return this
    }

    /**
     * Adds a mock computation that provides computed results for the specified contract during testing.
     *
     * This method creates a mock transformer that provides computation results when other transformers
     * request computation through the specified contract. The result is provided by the lambda function
     * which is called each time the computation is requested.
     *
     * @param C The computation contract type
     * @param D The type of data returned by the computation
     * @param contract The computation contract to mock
     * @param data Lambda function that provides the computation result
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .computation(AnalyticsService.userStatsContract) {
     *         UserStats(loginCount = 5, lastLogin = Date())
     *     }
     *     .testSignal(ProfileSignal.LoadStats) { /* assertions */ }
     * ```
     */
    fun <D : Any?> computation(
        contract: Contract.Computation<D>,
        data: () -> D
    ): TransmissionTest {
        mockTransformers += ComputationTransformer(contract, data)
        return this
    }

    /**
     * Adds a mock computation with arguments that provides computed results for the specified contract during testing.
     *
     * This method creates a mock transformer that provides computation results when other transformers
     * request computation with arguments through the specified contract. The result is provided by the lambda
     * function which is called each time the computation is requested with arguments.
     *
     * Note: Currently the arguments are not passed to the data lambda function due to the mock implementation.
     * This will be improved in future versions.
     *
     * @param C The computation contract type
     * @param D The type of data returned by the computation
     * @param A The type of arguments passed to the computation
     * @param contract The computation contract to mock
     * @param data Lambda function that provides the computation result
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .computationWithArgs(SearchService.searchUsersContract) {
     *         listOf(User("john", "john@example.com"))
     *     }
     *     .testSignal(SearchSignal.SearchUsers("john")) { /* assertions */ }
     * ```
     */
    fun <D : Any?, A : Any> computationWithArgs(
        contract: Contract.ComputationWithArgs<A, D>,
        data: () -> D
    ): TransmissionTest {
        mockTransformers += ComputationWithArgsTransformer(contract, data)
        return this
    }

    /**
     * Adds a checkpoint with arguments that will be validated during testing.
     *
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     *
     * This method sets up a checkpoint that will be automatically validated with the provided arguments
     * during test execution. The checkpoint validation happens after the main test transmission is processed.
     *
     * @param C The checkpoint contract type
     * @param A The type of arguments for checkpoint validation
     * @param checkpoint The checkpoint contract to validate
     * @param args The arguments to provide during checkpoint validation
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .checkpointWithArgs(ValidationCheckpoint.userConfirmation, ConfirmationData("approved"))
     *     .testSignal(PaymentSignal.ProcessPayment(amount)) {
     *         // Test that payment was processed after confirmation
     *         val paymentData = lastData<PaymentData.Processed>()
     *         assertNotNull(paymentData)
     *     }
     * ```
     */
    @ExperimentalTransmissionApi
    fun <A : Any> checkpointWithArgs(
        checkpoint: Contract.Checkpoint.WithArgs<A>,
        args: A
    ): TransmissionTest {
        mockTransformers += CheckpointWithArgsTransformer<A>(checkpoint, { args })
        orderedCheckpoints.plusAssign(checkpoint)
        return this
    }

    /**
     * Adds a default checkpoint that will be validated during testing.
     *
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     *
     * This method sets up a checkpoint that will be automatically validated during test execution.
     * The checkpoint validation happens after the main test transmission is processed.
     *
     * @param checkpoint The checkpoint contract to validate
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .checkpoint(SecurityCheckpoint.adminApproval)
     *     .testSignal(AdminSignal.DeleteUser(userId)) {
     *         // Test that user was deleted after admin approval
     *         val deletionData = lastData<UserData.Deleted>()
     *         assertNotNull(deletionData)
     *     }
     * ```
     */
    @ExperimentalTransmissionApi
    fun checkpoint(checkpoint: Contract.Checkpoint.Default): TransmissionTest {
        mockTransformers += CheckpointTransformer({ checkpoint })
        orderedCheckpoints.plusAssign(checkpoint)
        return this
    }

    /**
     * Sets up initial transmissions that should be processed before the main test transmission.
     *
     * This method allows you to establish initial state by processing signals and effects
     * before the main test transmission. This is useful for testing scenarios that require
     * specific pre-conditions or state setup.
     *
     * @param transmissions Variable number of transmissions to process initially
     * @return This TransmissionTest instance for method chaining
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .withInitialProcessing(
     *         UserSignal.Login("user", "password"),
     *         AppSignal.Initialize,
     *         DataSignal.LoadInitialData
     *     )
     *     .testSignal(UserSignal.UpdateProfile(newProfile)) {
     *         // Test profile update after login and initialization
     *         val profileData = lastData<ProfileData.Updated>()
     *         assertNotNull(profileData)
     *     }
     * ```
     */
    fun withInitialProcessing(vararg transmissions: Transmission): TransmissionTest {
        initialTransmissions.addAll(transmissions)
        return this
    }

    /**
     * Executes a test with the given effect transmission and runs the provided assertions.
     *
     * This method processes the effect through the configured router and captures all resulting
     * data and effect streams for assertion. The assertions are executed with access to the
     * captured streams through the [TestResult] receiver.
     *
     * @param effect The effect transmission to test
     * @param assertions Lambda with [TestResult] receiver for making assertions
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .testEffect(CacheEffect.Invalidate("user_data")) {
     *         // Assert that cache invalidation triggered cleanup
     *         val clearEffect = lastEffect<CacheEffect.Cleared>()
     *         assertNotNull(clearEffect)
     *
     *         // Assert no data was emitted
     *         assertTrue(dataStream.isEmpty())
     *     }
     * ```
     */
    fun testEffect(effect: Transmission.Effect, assertions: suspend TestResult.() -> Unit) {
        runTest(effect, assertions)
    }

    /**
     * Executes a test with the given signal transmission and runs the provided assertions.
     *
     * This method processes the signal through the configured router and captures all resulting
     * data and effect streams for assertion. The assertions are executed with access to the
     * captured streams through the [TestResult] receiver.
     *
     * @param signal The signal transmission to test
     * @param assertions Lambda with [TestResult] receiver for making assertions
     *
     * Example usage:
     * ```kotlin
     * transformer.test()
     *     .testSignal(UserSignal.Login("username", "password")) {
     *         // Assert successful login data
     *         val loginData = lastData<UserData.LoggedIn>()
     *         assertNotNull(loginData)
     *         assertEquals("username", loginData.user.username)
     *
     *         // Assert login effect was published
     *         val loginEffect = lastEffect<AuthEffect.LoginSuccessful>()
     *         assertNotNull(loginEffect)
     *     }
     * ```
     */
    fun testSignal(signal: Transmission.Signal, assertions: suspend TestResult.() -> Unit) {
        runTest(signal, assertions)
    }

    /** Internal method to run the actual test with any transmission type */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runTest(transmission: Transmission, assertions: suspend TestResult.() -> Unit) {
        router = TransmissionRouter {
            addDispatcher(dispatcher)
            addTransformerSet((listOf(transformer) + mockTransformers).toSet())
        }

        runTest {
            val dataStream: MutableList<Transmission.Data> = mutableListOf()
            val effectStream: MutableList<Transmission.Effect> = mutableListOf()

            try {
                backgroundScope.launch(UnconfinedTestDispatcher()) {
                    router!!.streamData().collect { data ->
                        dataStream.add(data)
                    }
                }
                backgroundScope.launch(UnconfinedTestDispatcher()) {
                    router!!.streamEffect().collect { effect ->
                        effectStream.add(effect)
                    }
                }

                // Process initial transmissions
                initialTransmissions.forEach {
                    when (it) {
                        is Transmission.Data ->
                            throw IllegalArgumentException(
                                "Transmission.Data should not be sent for processing"
                            )

                        is Transmission.Effect -> router!!.process(it)
                        is Transmission.Signal -> router!!.process(it)
                    }
                    transformer.waitProcessingToFinish()
                }

                // Process the test transmission
                when (transmission) {
                    is Transmission.Signal -> router!!.process(transmission)
                    is Transmission.Effect -> router!!.process(transmission)
                    else ->
                        throw IllegalArgumentException(
                            "Only Signal or Effect transmissions are supported for testing"
                        )
                }


                // Process checkpoints
                orderedCheckpoints.forEach {
                    when (it) {
                        is Contract.Checkpoint.Default -> router!!.process(DefaultCheckPoint)
                        is Contract.Checkpoint.WithArgs<*> ->
                            router!!.process(CheckpointWithArgs(it))
                    }
                    transformer.waitProcessingToFinish()
                }
                transformer.waitProcessingToFinish()
                advanceUntilIdle()

                // Run the assertions
                val testResult = TestResult(dataStream, effectStream)
                testResult.assertions()
            } finally {
                advanceUntilIdle()
                router?.clear()
            }
        }
    }

    /**
     * Contains the captured streams and provides assertion methods for test validation.
     *
     * TestResult is provided as the receiver in test assertion blocks and gives access to all
     * data and effects that were emitted during test execution. It provides various methods
     * for finding, filtering, and asserting on the captured transmissions.
     *
     * @param dataStream List of all data transmissions captured during test execution
     * @param effectStream List of all effect transmissions captured during test execution
     */
    class TestResult(
        val dataStream: List<Transmission.Data>,
        val effectStream: List<Transmission.Effect>
    ) {
        /**
         * Returns the last data transmission of the specified type.
         *
         * This is useful for asserting on the final state of a transformer after processing.
         *
         * @param T The type of data to retrieve
         * @return The last data of type T, or null if none exists
         *
         * Example:
         * ```kotlin
         * val userData = lastData<UserData.LoggedIn>()
         * assertNotNull(userData)
         * assertEquals("john", userData.username)
         * ```
         */
        @JvmName("lastDataWithType")
        inline fun <reified T : Transmission.Data> lastData(): T? =
            dataStream.filterIsInstance<T>().lastOrNull()

        /**
         * Returns the last data transmission of any type.
         *
         * @return The last data transmission, or null if none exists
         */
        fun lastData(): Transmission.Data? = dataStream.lastOrNull()

        /**
         * Returns the data transmission at the specified index.
         *
         * @param index Zero-based index of the data transmission to retrieve
         * @return The data at the specified index, or null if index is out of bounds
         *
         * Example:
         * ```kotlin
         * val firstData = nthData(0) // First data emitted
         * val secondData = nthData(1) // Second data emitted
         * ```
         */
        fun nthData(index: Int): Transmission.Data? = dataStream.getOrNull(index)

        /**
         * Returns the last effect transmission of the specified type.
         *
         * This is useful for asserting on side effects that should occur after processing.
         *
         * @param T The type of effect to retrieve
         * @return The last effect of type T, or null if none exists
         *
         * Example:
         * ```kotlin
         * val loginEffect = lastEffect<AuthEffect.LoginSuccessful>()
         * assertNotNull(loginEffect)
         * assertEquals("welcome_screen", loginEffect.targetScreen)
         * ```
         */
        @JvmName("lastEffectWithType")
        inline fun <reified T : Transmission.Effect> lastEffect(): T? =
            effectStream.filterIsInstance<T>().lastOrNull()

        /**
         * Returns the last effect transmission of any type.
         *
         * @return The last effect transmission, or null if none exists
         */
        fun lastEffect(): Transmission.Effect? = effectStream.lastOrNull()

        /**
         * Returns the effect transmission at the specified index.
         *
         * @param index Zero-based index of the effect transmission to retrieve
         * @return The effect at the specified index, or null if index is out of bounds
         *
         * Example:
         * ```kotlin
         * val firstEffect = nthEffect(0) // First effect emitted
         * val secondEffect = nthEffect(1) // Second effect emitted
         * ```
         */
        fun nthEffect(index: Int): Transmission.Effect? = effectStream.getOrNull(index)

        /**
         * Returns all data transmissions of the specified type.
         *
         * This is useful for asserting on multiple data emissions of the same type.
         *
         * @param T The type of data to retrieve
         * @return List of all data transmissions of type T
         *
         * Example:
         * ```kotlin
         * val allUserData = allData<UserData>()
         * assertEquals(3, allUserData.size)
         * assertTrue(allUserData.all { it.isValid })
         * ```
         */
        inline fun <reified T : Transmission.Data> allData(): List<T> =
            dataStream.filterIsInstance<T>()

        /**
         * Returns all effect transmissions of the specified type.
         *
         * This is useful for asserting on multiple effect emissions of the same type.
         *
         * @param T The type of effect to retrieve
         * @return List of all effect transmissions of type T
         *
         * Example:
         * ```kotlin
         * val allLogEvents = allEffects<LoggingEffect>()
         * assertTrue(allLogEvents.any { it.level == LogLevel.ERROR })
         * ```
         */
        inline fun <reified T : Transmission.Effect> allEffects(): List<T> =
            effectStream.filterIsInstance<T>()

        /**
         * Finds the first data transmission of the specified type that matches the predicate.
         *
         * @param T The type of data to search for
         * @param predicate Function to test each data transmission
         * @return The first matching data transmission, or null if none found
         *
         * Example:
         * ```kotlin
         * val errorData = findData<ApiData> { it.hasError }
         * assertNotNull(errorData)
         * assertEquals("Network timeout", errorData.errorMessage)
         * ```
         */
        inline fun <reified T : Transmission.Data> findData(predicate: (T) -> Boolean): T? =
            dataStream.filterIsInstance<T>().firstOrNull(predicate)

        /**
         * Finds the first effect transmission of the specified type that matches the predicate.
         *
         * @param T The type of effect to search for
         * @param predicate Function to test each effect transmission
         * @return The first matching effect transmission, or null if none found
         *
         * Example:
         * ```kotlin
         * val navigationEffect = findEffect<NavigationEffect> { it.destination == "profile" }
         * assertNotNull(navigationEffect)
         * assertTrue(navigationEffect.clearBackStack)
         * ```
         */
        inline fun <reified T : Transmission.Effect> findEffect(predicate: (T) -> Boolean): T? =
            effectStream.filterIsInstance<T>().firstOrNull(predicate)

        /**
         * Finds all data transmissions of the specified type that match the predicate.
         *
         * @param T The type of data to search for
         * @param predicate Function to test each data transmission
         * @return List of all matching data transmissions
         *
         * Example:
         * ```kotlin
         * val validUserData = findAllData<UserData> { it.isValid && it.hasProfile }
         * assertEquals(2, validUserData.size)
         * ```
         */
        inline fun <reified T : Transmission.Data> findAllData(predicate: (T) -> Boolean): List<T> =
            dataStream.filterIsInstance<T>().filter(predicate)

        /**
         * Finds all effect transmissions of the specified type that match the predicate.
         *
         * @param T The type of effect to search for
         * @param predicate Function to test each effect transmission
         * @return List of all matching effect transmissions
         *
         * Example:
         * ```kotlin
         * val errorEffects = findAllEffects<ErrorEffect> { it.severity == ErrorSeverity.HIGH }
         * assertTrue(errorEffects.isNotEmpty())
         * ```
         */
        inline fun <reified T : Transmission.Effect> findAllEffects(
            predicate: (T) -> Boolean
        ): List<T> = effectStream.filterIsInstance<T>().filter(predicate)

        /**
         * Checks if any data transmission of the specified type exists in the captured stream.
         *
         * @param T The type of data to check for
         * @return True if at least one data transmission of type T exists, false otherwise
         *
         * Example:
         * ```kotlin
         * assertTrue(hasData<UserData.LoggedIn>())
         * assertFalse(hasData<UserData.LoginFailed>())
         * ```
         */
        inline fun <reified T : Transmission.Data> hasData(): Boolean = dataStream.any { it is T }

        /**
         * Checks if any effect transmission of the specified type exists in the captured stream.
         *
         * @param T The type of effect to check for
         * @return True if at least one effect transmission of type T exists, false otherwise
         *
         * Example:
         * ```kotlin
         * assertTrue(hasEffect<NavigationEffect>())
         * assertFalse(hasEffect<ErrorEffect>())
         * ```
         */
        inline fun <reified T : Transmission.Effect> hasEffect(): Boolean =
            effectStream.any { it is T }
    }

    companion object {
        /**
         * Creates a new TransmissionTest instance for testing the specified transformer.
         *
         * This is the primary factory method for creating test instances. The test will use
         * [UnconfinedTestDispatcher] by default for immediate execution of coroutines.
         *
         * @param transformer The transformer to test
         * @return A new TransmissionTest instance
         *
         * Example:
         * ```kotlin
         * val test = TransmissionTest.forTransformer(UserTransformer())
         * test.testSignal(UserSignal.Login("user", "pass")) { /* assertions */ }
         * ```
         */
        fun forTransformer(transformer: Transformer): TransmissionTest {
            return TransmissionTest(transformer)
        }

        /**
         * Creates a new TransmissionTest instance for testing the specified transformer with a custom dispatcher.
         *
         * This method allows you to control coroutine execution timing by providing a custom test dispatcher.
         * This can be useful for testing time-dependent behavior or controlling execution order.
         *
         * @param transformer The transformer to test
         * @param dispatcher The test dispatcher to use for coroutine execution
         * @return A new TransmissionTest instance
         *
         * Example:
         * ```kotlin
         * val testDispatcher = StandardTestDispatcher()
         * val test = TransmissionTest.forTransformer(UserTransformer(), testDispatcher)
         * test.testSignal(UserSignal.DelayedOperation) {
         *     // Use testDispatcher.scheduler.advanceTimeBy() to control timing
         * }
         * ```
         */
        fun forTransformer(transformer: Transformer, dispatcher: TestDispatcher): TransmissionTest {
            return TransmissionTest(transformer, dispatcher)
        }
    }
}

/**
 * Extension function to create a TransmissionTest for this transformer.
 *
 * This provides a more readable and fluent API for creating tests directly from transformer instances.
 * Uses [UnconfinedTestDispatcher] for immediate coroutine execution.
 *
 * @receiver The transformer to test
 * @return A new TransmissionTest instance
 *
 * Example:
 * ```kotlin
 * val transformer = UserTransformer()
 * transformer.test()
 *     .withData(UserRepository.currentUserContract) { UserData.LoggedOut }
 *     .testSignal(UserSignal.Login("user", "pass")) {
 *         val loginData = lastData<UserData.LoggedIn>()
 *         assertNotNull(loginData)
 *     }
 * ```
 */
fun Transformer.test(): TransmissionTest {
    return TransmissionTest.forTransformer(this)
}

/**
 * Extension function to create a TransmissionTest for this transformer with a custom dispatcher.
 *
 * This provides a more readable and fluent API for creating tests with controlled coroutine execution.
 * Useful for testing time-dependent behavior or controlling execution order.
 *
 * @receiver The transformer to test
 * @param dispatcher The test dispatcher to use for coroutine execution
 * @return A new TransmissionTest instance
 *
 * Example:
 * ```kotlin
 * val testDispatcher = StandardTestDispatcher()
 * val transformer = TimerTransformer()
 * transformer.test(testDispatcher)
 *     .testSignal(TimerSignal.Start(5000)) {
 *         // Control timing with testDispatcher.scheduler
 *         testDispatcher.scheduler.advanceTimeBy(5000)
 *         val timerData = lastData<TimerData.Finished>()
 *         assertNotNull(timerData)
 *     }
 * ```
 */
fun Transformer.test(dispatcher: TestDispatcher): TransmissionTest {
    return TransmissionTest.forTransformer(this, dispatcher)
}

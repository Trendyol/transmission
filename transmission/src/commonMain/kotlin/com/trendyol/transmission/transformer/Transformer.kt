package com.trendyol.transmission.transformer

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.effect.RouterEffectWithType
import com.trendyol.transmission.effect.WrappedEffect
import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.transformer.checkpoint.CheckpointTracker
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.QueryType
import com.trendyol.transmission.transformer.request.TransformerQueryDelegate
import com.trendyol.transmission.transformer.request.computation.ComputationRegistry
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.execution.ExecutionRegistry
import com.trendyol.transmission.transformer.request.execution.Executions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Base class for implementing business logic processors in the Transmission framework.
 * 
 * Transformers are the core processing units that handle [Transmission.Signal]s and [Transmission.Effect]s,
 * converting them into [Transmission.Data] or other effects. Each transformer operates independently with its
 * own coroutine scope and can maintain internal state through data holders.
 * 
 * Key capabilities:
 * - Handle signals from UI/external sources
 * - Process effects from other transformers
 * - Emit data representing application state
 * - Maintain internal state with data holders
 * - Communicate with other transformers via queries
 * - Support checkpoint-based flow control
 * 
 * @param identity Unique identifier for this transformer instance. Used for targeted communication.
 * @param dispatcher Coroutine dispatcher for this transformer's operations. Defaults to [Dispatchers.Default].
 * @param capacity Buffer capacity for internal channels. Affects memory usage and backpressure handling.
 * 
 * Example implementation:
 * ```kotlin
 * class UserTransformer : Transformer() {
 *     private val userState = dataHolder(UserState())
 *     
 *     override val handlers: Handlers = handlers {
 *         onSignal<UserSignal.Login> { signal ->
 *             val user = authenticateUser(signal.credentials)
 *             userState.update { it.copy(currentUser = user) }
 *             send(UserData.LoggedIn(user))
 *         }
 *         
 *         onEffect<UserEffect.Logout> {
 *             userState.update { it.copy(currentUser = null) }
 *             send(UserData.LoggedOut)
 *         }
 *     }
 * }
 * ```
 * 
 * @see handlers for defining signal and effect processing
 * @see computations for exposing query-able computations
 * @see executions for exposing executable operations
 * @see com.trendyol.transmission.transformer.dataholder.dataHolder for state management
 */
@OptIn(ExperimentalTransmissionApi::class)
open class Transformer(
    identity: Contract.Identity = Contract.identity(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val capacity: Capacity = Capacity.Default
) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
    }

    @PublishedApi
    internal val transformerScope = CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)

    private val _identity: Contract.Identity = identity

    private val effectChannel: Channel<WrappedEffect> = Channel(capacity = capacity.value)
    internal val dataChannel: Channel<Transmission.Data> = Channel(capacity = capacity.value)

    private var checkpointProvider: () -> CheckpointTracker? = { null }
    private val requestDelegate by lazy {
        TransformerQueryDelegate(
            checkpointTrackerProvider = checkpointProvider,
            identity = _identity,
            capacity = capacity,
            scope = transformerScope,
        )
    }

    internal val storage = TransformerStorage()

    internal val handlerRegistry by lazy { HandlerRegistry() }
    internal val executionRegistry: ExecutionRegistry by lazy { ExecutionRegistry(this) }
    internal val computationRegistry: ComputationRegistry by lazy {
        ComputationRegistry(this)
    }

    /**
     * Defines how this transformer handles [Transmission.Signal]s and [Transmission.Effect]s.
     * 
     * Override this property to specify the processing logic for different signal and effect types.
     * The handlers DSL provides type-safe methods for registering signal and effect processors.
     * 
     * Example:
     * ```kotlin
     * override val handlers: Handlers = handlers {
     *     onSignal<MySignal> { signal ->
     *         // Process signal and emit data
     *         send(MyData(signal.value))
     *     }
     *     onEffect<MyEffect> { effect ->
     *         // Handle effect from another transformer
     *         publish(AnotherEffect(effect.data))
     *     }
     * }
     * ```
     * 
     * @see com.trendyol.transmission.transformer.handler.handlers
     * @see com.trendyol.transmission.transformer.handler.onSignal
     * @see com.trendyol.transmission.transformer.handler.onEffect
     */
    protected open val handlers: Handlers by lazy { Handlers() }
    
    /**
     * Defines computations that other transformers can query from this transformer.
     * 
     * Computations provide a way for transformers to expose data or calculations that
     * other transformers can request. Results can be cached to improve performance.
     * 
     * Example:
     * ```kotlin
     * override val computations: Computations = computations {
     *     register(userCountContract) {
     *         userRepository.getUserCount()
     *     }
     *     register(userByIdContract) { userId ->
     *         userRepository.findById(userId)
     *     }
     * }
     * ```
     * 
     * @see com.trendyol.transmission.transformer.request.computation.computations
     * @see com.trendyol.transmission.transformer.request.computation.register
     */
    protected open val computations: Computations by lazy { Computations() }
    
    /**
     * Defines executable operations that other transformers can trigger on this transformer.
     * 
     * Executions provide a way for transformers to expose operations that other transformers
     * can invoke remotely, useful for triggering side effects or coordinated actions.
     * 
     * Example:
     * ```kotlin
     * override val executions: Executions = executions {
     *     register(clearCacheContract) {
     *         cache.clear()
     *     }
     *     register(sendNotificationContract) { message ->
     *         notificationService.send(message)
     *     }
     * }
     * ```
     * 
     * @see com.trendyol.transmission.transformer.request.execution.executions  
     * @see com.trendyol.transmission.transformer.request.execution.register
     */
    protected open val executions: Executions by lazy { Executions() }

    var currentEffectProcessing: Job? = null
    var currentSignalProcessing: Job? = null

    val communicationScope: CommunicationScope by lazy {
        CommunicationScopeImpl(
            effectChannel = effectChannel,
            dataChannel = dataChannel,
            queryDelegate = requestDelegate
        )
    }

    internal fun bindCheckpointTracker(tracker: CheckpointTracker) {
        checkpointProvider = { tracker }
    }

    internal fun startSignalCollection(incoming: SharedFlow<Transmission.Signal>) {
        transformerScope.launch {
            incoming.collect {
                currentSignalProcessing = transformerScope.launch {
                    handlerRegistry.signalHandlerRegistry[it::class]?.execute(
                        communicationScope,
                        it
                    )
                }
            }
        }
    }

    internal fun startDataPublishing(data: SendChannel<Transmission.Data>) {
        transformerScope.launch { dataChannel.receiveAsFlow().collect { data.send(it) } }
    }

    internal fun startEffectProcessing(
        producer: SendChannel<WrappedEffect>,
        incoming: SharedFlow<WrappedEffect>
    ) {
        transformerScope.launch {
            supervisorScope {
                launch {
                    incoming
                        .filterNot { it.effect is RouterEffect }
                        .filterNot { it.effect is RouterEffectWithType<*> }
                        .filter { it.identity == null || it.identity == _identity }
                        .map { it.effect }
                        .collect {
                            currentEffectProcessing = launch {
                                handlerRegistry.effectHandlerRegistry[it::class]?.execute(
                                    communicationScope,
                                    it
                                )
                            }
                        }
                }
                launch {
                    effectChannel.receiveAsFlow().collect { producer.send(it) }
                }
            }
        }
    }

    internal fun startQueryProcessing(
        incomingQuery: SharedFlow<QueryResult>,
        outGoingQuery: SendChannel<QueryType>
    ) {
        transformerScope.launch {
            supervisorScope {
                launch {
                    incomingQuery
                        .filter { it.owner == _identity.key }
                        .collect {
                            this@Transformer.requestDelegate.resultBroadcast.producer.send(it)
                        }
                }
                launch {
                    this@Transformer.requestDelegate.outGoingQuery.receiveAsFlow().collect {
                        outGoingQuery.send(it)
                    }
                }
            }
        }
    }

    /**
     * Called when this transformer is being cleared and resources should be released.
     * 
     * Override this method to perform cleanup operations such as:
     * - Closing database connections
     * - Cancelling background operations
     * - Releasing resources
     * - Saving state
     * 
     * This method is called before the transformer's coroutine scope is cancelled,
     * so it's safe to launch final coroutines if needed.
     * 
     * Example:
     * ```kotlin
     * override fun onCleared() {
     *     super.onCleared()
     *     database.close()
     *     preferences.save()
     * }
     * ```
     */
    open fun onCleared() {}

    /**
     * Clears this transformer by calling [onCleared], cancelling its coroutine scope, and clearing storage.
     * 
     * This method should be called when the transformer is no longer needed to ensure proper resource cleanup.
     * After calling this method, the transformer should not be used anymore.
     * 
     * The cleanup process:
     * 1. Calls [onCleared] for custom cleanup logic
     * 2. Cancels the transformer's coroutine scope
     * 3. Clears internal storage
     * 
     * @see onCleared for custom cleanup logic
     */
    fun clear() {
        onCleared()
        transformerScope.cancel()
        storage.clear()
    }

    /**
     * Called when an uncaught exception occurs within this transformer's coroutine scope.
     * 
     * Override this method to implement custom error handling logic such as:
     * - Logging errors
     * - Reporting crashes
     * - Recovery mechanisms
     * - Notifying other components
     * 
     * By default, this method does nothing (errors are silently ignored).
     * 
     * @param throwable The uncaught exception that occurred
     * 
     * Example:
     * ```kotlin
     * override fun onError(throwable: Throwable) {
     *     super.onError(throwable)
     *     logger.error("Transformer error", throwable)
     *     crashReporter.report(throwable)
     * }
     * ```
     */
    open fun onError(throwable: Throwable) {
        // no-operation
    }
}

package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.WrappedEffect
import com.trendyol.transmission.router.builder.TransmissionRouterBuilderScope
import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.checkpoint.CheckpointTracker
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * Central routing service that manages signal processing and data flow in Transmission applications.
 * 
 * TransmissionRouter acts as the coordination hub for all transformers in the system. It receives
 * [Transmission.Signal]s from UI components, routes them to appropriate transformers, manages
 * [Transmission.Effect] propagation between transformers, and streams [Transmission.Data] back to observers.
 * 
 * Key responsibilities:
 * - Route incoming signals to registered transformers
 * - Manage effect propagation between transformers
 * - Coordinate data streaming to observers
 * - Handle transformer lifecycle and cleanup
 * - Provide query-based communication between transformers
 * 
 * The router operates asynchronously using coroutines and provides backpressure handling
 * through configurable channel capacities.
 * 
 * @param identity Unique identifier for this router instance
 * @param transformerSetLoader Optional loader for transformer initialization
 * @param autoInitialization Whether to automatically initialize transformers on creation
 * @param capacity Buffer capacity for internal channels
 * @param dispatcher Coroutine dispatcher for router operations
 * 
 * @throws IllegalStateException when supplied [Transformer] set is empty during initialization
 * 
 * Example usage:
 * ```kotlin
 * val router = TransmissionRouter {
 *     addTransformerSet(setOf(userTransformer, dataTransformer))
 *     setCapacity(Capacity.Custom(128))
 * }
 * 
 * // Process signals
 * router.process(UserSignal.Login(credentials))
 * 
 * // Observe data
 * launch {
 *     router.streamData<UserData>().collect { userData ->
 *         updateUI(userData)
 *     }
 * }
 * ```
 * 
 * @see Transformer for implementing business logic
 * @see streamData for observing data streams
 * @see process for sending signals and effects
 */
class TransmissionRouter internal constructor(
    identity: Contract.Identity,
    internal val transformerSetLoader: TransformerSetLoader? = null,
    internal val autoInitialization: Boolean = true,
    internal val capacity: Capacity = Capacity.Default,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): StreamOwner {

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)

    private val _transformerSet: MutableSet<Transformer> = mutableSetOf()
    internal val transformerSet: Set<Transformer> = _transformerSet

    internal val routerName: String = identity.key

    private val signalBroadcast =
        routerScope.createBroadcast<Transmission.Signal>(capacity = capacity)
    private val dataBroadcast = routerScope.createBroadcast<Transmission.Data>(capacity = capacity)
    private val effectBroadcast = routerScope.createBroadcast<WrappedEffect>(capacity = capacity)

    private val checkpointTracker = CheckpointTracker()

    override val dataStream = dataBroadcast.output

    override val effectStream: SharedFlow<Transmission.Effect> = effectBroadcast.output
        .map { it.effect }.shareIn(routerScope, SharingStarted.Lazily)

    private val _queryManager = QueryManager(
        queryScope = routerScope,
        routerRef = this@TransmissionRouter,
        capacity = capacity,
    )

    /**
     * Provides access to the query system for inter-transformer communication.
     * 
     * The query helper allows transformers to communicate with each other through
     * type-safe queries, enabling computations and executions across transformer boundaries.
     * 
     * @see QueryHandler for available query operations
     * @see com.trendyol.transmission.transformer.request.Contract for defining query contracts
     */
    val queryHelper: QueryHandler = _queryManager.handler

    init {
        if (autoInitialization) {
            initializeInternal(transformerSetLoader)
        }
    }

    /**
     * Manually initializes the router with the specified [TransformerSetLoader].
     * 
     * This method is only available when auto-initialization is disabled via 
     * [TransmissionRouterBuilderScope.overrideInitialization]. It allows for deferred
     * initialization of transformers, which can be useful for dependency injection
     * scenarios or when transformers need to be loaded dynamically.
     * 
     * @param loader The transformer set loader containing the transformers to initialize
     * 
     * @throws IllegalStateException if auto-initialization is enabled
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     overrideInitialization()
     * }
     * 
     * // Later, when transformers are ready
     * router.initialize(MyTransformerSetLoader())
     * ```
     * 
     * @see TransmissionRouterBuilderScope.overrideInitialization
     * @see TransformerSetLoader
     */
    fun initialize(loader: TransformerSetLoader) {
        check(!autoInitialization) {
            "TransmissionRouter is configured to initialize automatically."
        }
        initializeInternal(loader)
    }

    /**
     * Processes a [Transmission.Signal] by routing it to all registered transformers.
     * 
     * Signals represent user interactions or external events that need to be processed
     * by the application. This method broadcasts the signal to all transformers that
     * have registered handlers for the signal type.
     * 
     * The processing is asynchronous and non-blocking. Signals are queued in an internal
     * channel with the configured capacity.
     * 
     * @param signal The signal to process
     * 
     * Example usage:
     * ```kotlin
     * router.process(UserSignal.Login(username, password))
     * router.process(DataSignal.Refresh)
     * ```
     * 
     * @see Transmission.Signal
     * @see com.trendyol.transmission.transformer.handler.onSignal
     */
    fun process(signal: Transmission.Signal) {
        routerScope.launch {
            signalBroadcast.producer.send(signal)
        }
    }

    /**
     * Processes a [Transmission.Effect] by routing it to appropriate transformers.
     * 
     * Effects represent side effects or inter-transformer communications that need to be
     * processed. This method broadcasts the effect to transformers that have registered
     * handlers for the effect type.
     * 
     * The processing is asynchronous and non-blocking. Effects are queued in an internal
     * channel with the configured capacity.
     * 
     * @param effect The effect to process
     * 
     * Example usage:
     * ```kotlin
     * router.process(NetworkEffect.ConnectionLost)
     * router.process(CacheEffect.Invalidate("user-data"))
     * ```
     * 
     * @see Transmission.Effect
     * @see com.trendyol.transmission.transformer.handler.onEffect
     */
    fun process(effect: Transmission.Effect) {
        routerScope.launch {
            effectBroadcast.producer.send(WrappedEffect(effect))
        }
    }

    private fun initializeInternal(transformerSetLoader: TransformerSetLoader?) {
        routerScope.launch {
            transformerSetLoader?.load()?.let { _transformerSet.addAll(it) }
            initializeTransformers(transformerSet)
        }
    }

    private fun initializeTransformers(transformerSet: Set<Transformer>) {
        check(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        transformerSet.forEach { transformer ->
            transformer.run {
                bindCheckpointTracker(checkpointTracker)
                startSignalCollection(incoming = signalBroadcast.output)
                startDataPublishing(data = dataBroadcast.producer)
                startEffectProcessing(
                    producer = effectBroadcast.producer,
                    incoming = effectBroadcast.output,
                )
                startQueryProcessing(
                    incomingQuery = _queryManager.incomingQueryResponse,
                    outGoingQuery = _queryManager.outGoingQuery
                )
            }
        }
    }

    /**
     * Clears the router and all its transformers, releasing all resources.
     * 
     * This method should be called when the router is no longer needed to ensure
     * proper cleanup of resources. It performs the following operations:
     * 1. Clears all registered transformers
     * 2. Cancels the router's coroutine scope
     * 3. Closes all internal channels
     * 
     * After calling this method, the router should not be used anymore.
     * 
     * Example usage:
     * ```kotlin
     * // In a ViewModel or similar lifecycle-aware component
     * override fun onCleared() {
     *     super.onCleared()
     *     router.clear()
     * }
     * ```
     * 
     * @see Transformer.clear
     */
    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
    }
}


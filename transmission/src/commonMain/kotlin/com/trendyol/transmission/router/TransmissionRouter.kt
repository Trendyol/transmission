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
 * Throws [IllegalStateException] when supplied [Transformer] set is empty
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

    val queryHelper: QueryHandler = _queryManager.handler

    init {
        if (autoInitialization) {
            initializeInternal(transformerSetLoader)
        }
    }

    /**
     * Initializes the [TransmissionRouter] with the corresponding [TransformerSetLoader].
     * Default behaviour of TransmissionRouter is to be initialized automatically. To
     * override this behaviour, you must call [TransmissionRouterBuilderScope.overrideInitialization].
     * Otherwise this method throws [IllegalStateException]
     */
    fun initialize(loader: TransformerSetLoader) {
        check(!autoInitialization) {
            "TransmissionRouter is configured to initialize automatically."
        }
        initializeInternal(loader)
    }

    fun process(signal: Transmission.Signal) {
        routerScope.launch {
            signalBroadcast.producer.send(signal)
        }
    }

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

    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
    }
}


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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
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
) {

    private val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)

    private val _transformerSet: MutableSet<Transformer> = mutableSetOf()
    internal val transformerSet: Set<Transformer> = _transformerSet

    internal val routerName: String = identity.key

    private val _signalStream =
        MutableSharedFlow<Transmission.Signal>(extraBufferCapacity = capacity.value)
    private val _dataStream =
        MutableSharedFlow<Transmission.Data>(extraBufferCapacity = capacity.value)
    private val _effectStream =
        MutableSharedFlow<WrappedEffect>(extraBufferCapacity = capacity.value)

    private val checkpointTracker = CheckpointTracker()

    @PublishedApi
    internal val dataStream = _dataStream.asSharedFlow()

    @PublishedApi
    internal val effectStream: SharedFlow<Transmission.Effect> = _effectStream
        .map { it.effect }.shareIn(routerScope, SharingStarted.Lazily)

    private val _queryManager = QueryManager(
        queryScope = routerScope,
        routerRef = this@TransmissionRouter,
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
            _signalStream.emit(signal)
        }
    }

    fun process(effect: Transmission.Effect) {
        routerScope.launch {
            _effectStream.emit(WrappedEffect(effect))
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
                startSignalCollection(incoming = _signalStream)
                startDataPublishing(data = _dataStream)
                startEffectProcessing(
                    producer = _effectStream,
                    incoming = _effectStream,
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


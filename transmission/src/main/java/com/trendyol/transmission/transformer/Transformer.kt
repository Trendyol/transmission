package com.trendyol.transmission.transformer

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffect
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

@OptIn(ExperimentalTransmissionApi::class)
open class Transformer(
    identity: Contract.Identity = Contract.identity(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val capacity = Capacity.Default

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
    }

    @PublishedApi
    internal val transformerScope = CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)

    private val _identity: Contract.Identity = identity

    private val effectChannel =
        MutableSharedFlow<WrappedEffect>(extraBufferCapacity = capacity.value)
    internal val dataChannel =
        MutableSharedFlow<Transmission.Data>(extraBufferCapacity = capacity.value)

    private var checkpointProvider: () -> CheckpointTracker? = { null }
    private val requestDelegate by lazy {
        TransformerQueryDelegate(
            checkpointTrackerProvider = checkpointProvider,
            identity = _identity,
            capacity = capacity,
        )
    }

    internal val storage = TransformerStorage()

    internal val handlerRegistry by lazy { HandlerRegistry() }
    internal val executionRegistry: ExecutionRegistry by lazy { ExecutionRegistry(this) }
    internal val computationRegistry: ComputationRegistry by lazy {
        ComputationRegistry(this)
    }

    protected open val handlers: Handlers by lazy { Handlers() }
    protected open val computations: Computations by lazy { Computations() }
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
                    handlerRegistry.signalHandlerRegistry[it::class]?.invoke(
                        communicationScope,
                        it
                    )
                }
            }
        }
    }

    internal fun startDataPublishing(data: MutableSharedFlow<Transmission.Data>) {
        transformerScope.launch { dataChannel.collect { data.emit(it) } }
    }

    internal fun startEffectProcessing(
        producer: MutableSharedFlow<WrappedEffect>,
        incoming: SharedFlow<WrappedEffect>
    ) {
        transformerScope.launch {
            supervisorScope {
                launch {
                    incoming
                        .filterNot { it.effect is RouterEffect }
                        .filter { it.identity == null || it.identity == _identity }
                        .map { it.effect }
                        .collect {
                            currentEffectProcessing = launch {
                                handlerRegistry.effectHandlerRegistry[it::class]
                                    ?.invoke(communicationScope, it)
                            }
                        }
                }
                launch {
                    effectChannel.collect { producer.emit(it) }
                }
            }
        }
    }

    internal fun startQueryProcessing(
        incomingQuery: SharedFlow<QueryResult>,
        outGoingQuery: MutableSharedFlow<QueryType>
    ) {
        transformerScope.launch {
            supervisorScope {
                launch {
                    incomingQuery
                        .filter { it.owner == _identity.key }
                        .collect {
                            this@Transformer.requestDelegate.resultBroadcast.emit(it)
                        }
                }
                launch {
                    this@Transformer.requestDelegate.outGoingQuery.collect {
                        outGoingQuery.emit(it)
                    }
                }
            }
        }
    }

    fun clear() {
        transformerScope.cancel()
        storage.clear()
    }

    open fun onError(throwable: Throwable) {
        // no-operation
    }
}

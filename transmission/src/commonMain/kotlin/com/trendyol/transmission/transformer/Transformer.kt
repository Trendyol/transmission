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

    open fun onCleared() {}

    fun clear() {
        onCleared()
        transformerScope.cancel()
        storage.clear()
    }

    open fun onError(throwable: Throwable) {
        // no-operation
    }
}

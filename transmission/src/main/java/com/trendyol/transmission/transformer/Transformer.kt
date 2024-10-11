package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.ExtendedHandlers
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.Query
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.TransformerRequestDelegate
import com.trendyol.transmission.transformer.request.computation.ComputationRegistry
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.ExtendedComputations
import com.trendyol.transmission.transformer.request.execution.ExecutionRegistry
import com.trendyol.transmission.transformer.request.execution.Executions
import com.trendyol.transmission.transformer.request.execution.ExtendedExecutions
import com.trendyol.transmission.transformer.request.identity
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

open class Transformer(
    identity: Contract.Identity = Contracts.identity(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        onError(throwable)
    }

    @PublishedApi
    internal val transformerScope = CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)

    private val _identity: Contract.Identity = identity

    private val effectChannel: Channel<EffectWrapper> = Channel(capacity = Channel.BUFFERED)
    private val requestDelegate = TransformerRequestDelegate(transformerScope, _identity)
    internal val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.BUFFERED)
    internal val storage = TransformerStorage()

    internal val handlerRegistry by lazy { HandlerRegistry() }
    internal val executionRegistry: ExecutionRegistry by lazy { ExecutionRegistry(this) }
    internal val computationRegistry: ComputationRegistry by lazy {
        ComputationRegistry(this)
    }

    protected open val handlers: Handlers by lazy { Handlers() }
    protected open val extendedHandlers: ExtendedHandlers by lazy { ExtendedHandlers() }

    protected open val computations: Computations by lazy { Computations() }
    protected open val extendedComputations: ExtendedComputations by lazy { ExtendedComputations() }

    protected open val executions: Executions by lazy { Executions() }
    protected open val extendedExecutions: ExtendedExecutions by lazy { ExtendedExecutions() }

    var currentEffectProcessing: Job? = null
    var currentSignalProcessing: Job? = null

    val communicationScope: CommunicationScope = CommunicationScopeBuilder(
        effectChannel = effectChannel,
        dataChannel = dataChannel,
        requestDelegate = requestDelegate
    )

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

    internal fun startDataPublishing(data: SendChannel<Transmission.Data>) {
        transformerScope.launch { dataChannel.receiveAsFlow().collect { data.send(it) } }
    }

    internal fun startEffectProcessing(
        producer: SendChannel<EffectWrapper>,
        incoming: SharedFlow<EffectWrapper>
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
                    effectChannel.receiveAsFlow().collect { producer.send(it) }
                }
            }
        }
    }

    internal fun startQueryProcessing(
        incomingQuery: SharedFlow<QueryResult>,
        outGoingQuery: SendChannel<Query>
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

    fun clear() {
        transformerScope.cancel()
        storage.clear()
    }

    open fun onError(throwable: Throwable) {
        // no-operation
    }
}

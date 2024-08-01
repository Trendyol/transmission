package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.Query
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.TransformerRequestDelegate
import com.trendyol.transmission.transformer.request.computation.ComputationRegistry
import com.trendyol.transmission.transformer.request.createIdentity
import com.trendyol.transmission.transformer.request.execution.ExecutionRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

open class Transformer(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    identity: Contract.Identity? = null
) {

    val transformerScope = CoroutineScope(dispatcher)

    private val internalIdentity: Contract.Identity =
        identity ?: createIdentity(this::class.simpleName.orEmpty())

    private val effectChannel: Channel<EffectWrapper> = Channel(capacity = Channel.BUFFERED)
    private val requestDelegate = TransformerRequestDelegate(transformerScope, internalIdentity)
    internal val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.BUFFERED)
    internal val storage = TransformerStorage()

    protected open val handlerRegistry : HandlerRegistry? = null

    protected val executionRegistry: ExecutionRegistry by lazy { ExecutionRegistry(this) }
    protected val computationRegistry: ComputationRegistry by lazy { ComputationRegistry(this) }

    var currentEffectProcessing: Job? = null
    var currentSignalProcessing: Job? = null

    val communicationScope: CommunicationScope = CommunicationScopeBuilder(
        effectChannel = effectChannel,
        dataChannel = dataChannel,
        requestDelegate = requestDelegate
    )

    fun startSignalCollection(incoming: SharedFlow<Transmission.Signal>) {
        transformerScope.launch {
            incoming.collect {
                currentSignalProcessing = launch {
                    handlerRegistry?.signalHandlerRegistry?.get(it::class)?.invoke(communicationScope, it)
                }
            }
        }
    }

    fun startDataPublishing(data: SendChannel<Transmission.Data>) {
        transformerScope.launch { dataChannel.receiveAsFlow().collect { data.send(it) } }
    }

    fun startEffectProcessing(
        producer: SendChannel<EffectWrapper>,
        incoming: SharedFlow<EffectWrapper>
    ) {
        transformerScope.launch {
            launch {
                incoming
                    .filterNot { it.effect is RouterEffect }
                    .filter { it.identity == null || it.identity == internalIdentity }
                    .map { it.effect }
                    .collect {
                        currentEffectProcessing = launch {
                            handlerRegistry?.effectHandlerRegistry?.get(it::class)
                                ?.invoke(communicationScope, it)
                        }
                    }
            }
            launch {
                effectChannel.receiveAsFlow().collect { producer.send(it) }
            }
        }
    }

    fun startQueryProcessing(
        incomingQuery: SharedFlow<QueryResult>,
        outGoingQuery: SendChannel<Query>
    ) {
        transformerScope.launch {
            launch {
                incomingQuery
                    .filter { it.owner == internalIdentity.key }
                    .collect {
                        this@Transformer.requestDelegate.resultBroadcast.producer.trySend(it)
                    }
            }
            launch {
                this@Transformer.requestDelegate.outGoingQuery.receiveAsFlow().collect {
                    outGoingQuery.trySend(it)
                }
            }
        }
    }

    fun clear() {
        transformerScope.cancel()
        storage.clear()
    }
}

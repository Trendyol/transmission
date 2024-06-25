package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.identifier
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.request.Query
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.TransformerQueryDelegate
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

open class Transformer(dispatcher: CoroutineDispatcher = Dispatchers.Default) {

    val transformerScope = CoroutineScope(dispatcher)

    private val identifier: String = this.identifier()

    private val effectChannel: Channel<EffectWrapper> = Channel(capacity = Channel.BUFFERED)
    private val queryDelegate = TransformerQueryDelegate(transformerScope, identifier)
    internal val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.BUFFERED)
    internal val storage = TransformerStorage()

    open val signalHandler: SignalHandler? = null
    open val effectHandler: EffectHandler? = null

    private var currentEffectProcessing: Job? = null
    private var currentSignalProcessing: Job? = null

    suspend fun waitProcessingToFinish() {
        currentSignalProcessing?.join()
        currentEffectProcessing?.join()
    }

    val communicationScope: CommunicationScope = CommunicationScopeBuilder(
        effectChannel = effectChannel,
        dataChannel = dataChannel,
        queryDelegate = queryDelegate
    )

    fun startSignalCollection(incoming: SharedFlow<Transmission.Signal>) {
        transformerScope.launch {
            incoming.collect {
                signalHandler?.apply {
                    currentSignalProcessing = launch {
                        communicationScope.onSignal(it)
                    }
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
                    .filter { it.receiver == null || it.receiver == this@Transformer::class }
                    .map { it.effect }
                    .collect {
                        effectHandler?.apply {
                            currentEffectProcessing = launch {
                                communicationScope.onEffect(it)
                            }
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
                    .filter { it.owner == identifier }
                    .collect {
                        this@Transformer.queryDelegate.resultBroadcast.producer.trySend(it)
                    }
            }
            launch {
                this@Transformer.queryDelegate.outGoingQuery.receiveAsFlow().collect {
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

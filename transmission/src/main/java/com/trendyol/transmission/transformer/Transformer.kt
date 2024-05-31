package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.identifier
import com.trendyol.transmission.router.QueryDelegate
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.query.TransformerQueryDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

open class Transformer(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {

    val transformerScope = CoroutineScope(dispatcher)

    val identifier: String = this.identifier()

    private val effectChannel: Channel<EffectWrapper> = Channel(capacity = Channel.BUFFERED)
    private val queryDelegate = TransformerQueryDelegate(transformerScope, identifier)
    internal val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.BUFFERED)
    internal val storage = TransformerStorage()

    open val signalHandler: SignalHandler? = null
    open val effectHandler: EffectHandler? = null

    val communicationScope: CommunicationScope = object : CommunicationScope {

        override fun <D : Transmission.Data> send(data: D?) {
            data?.let { dataChannel.trySend(it) }
        }

        override fun <E : Transmission.Effect, T : Transformer> send(effect: E, to: KClass<out T>) {
            effectChannel.trySend(EffectWrapper(effect, to))
        }

        override fun <E : Transmission.Effect> publish(effect: E) {
            effectChannel.trySend(EffectWrapper(effect))
        }

        override suspend fun <D : Transmission.Data> queryData(
            type: KClass<D>,
            owner: KClass<out Transformer>?
        ): D? {
            return queryDelegate.interactor.queryData(type, owner)
        }

        override suspend fun <D : Transmission.Data, T : Transformer> queryComputation(
            type: KClass<D>,
            owner: KClass<out T>,
            invalidate: Boolean
        ): D? {
            return queryDelegate.interactor.queryComputation(type, owner, invalidate)
        }
    }

    suspend fun startSignalCollection(incoming: SharedFlow<Transmission.Signal>) {
        coroutineScope {
            incoming.collect {
                signalHandler?.apply {
                    launch {
                        communicationScope.onSignal(it)
                    }
                }
            }
        }
    }

    suspend fun startDataPublishing(data: SendChannel<Transmission.Data>) {
        coroutineScope {
            launch { dataChannel.receiveAsFlow().collect { data.send(it) } }
        }
    }

    suspend fun startEffectProcessing(
        producer: SendChannel<EffectWrapper>,
        incoming: SharedFlow<EffectWrapper>
    ) {
        coroutineScope {
            launch {
                incoming
                    .filterNot { it.effect is RouterEffect }
                    .filter { it.receiver == null || it.receiver == this@Transformer::class }
                    .map { it.effect }
                    .collect {
                        effectHandler?.apply {
                            launch { communicationScope.onEffect(it) }
                        }
                    }
            }
            launch {
                effectChannel.receiveAsFlow().collect { producer.send(it) }
            }
        }
    }

    internal suspend fun startQueryProcessing(queryDelegate: QueryDelegate) {
        coroutineScope {
            launch {
                queryDelegate.incomingQueryResponse
                    .filter { it.owner == identifier }
                    .collect {
                        this@Transformer.queryDelegate.resultBroadcast.producer.trySend(it)
                    }
            }
            launch {
                this@Transformer.queryDelegate.outGoingQuery.receiveAsFlow().collect {
                    queryDelegate.outGoingQuery.send(it)
                }
            }
        }
    }

    fun clear() {
        transformerScope.cancel()
        storage.clear()
    }
}

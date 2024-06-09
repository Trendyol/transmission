package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

internal class TestRouter(
    private val registry: RegistryScopeImpl,
    private val transformer: Transformer,
    private val dispatcher: CoroutineDispatcher
) {
    private val testScope = CoroutineScope(dispatcher)

    private val signalBroadcast = testScope.createBroadcast<Transmission.Signal>()
    private val effectBroadcast = testScope.createBroadcast<EffectWrapper>()

    fun sendSignal(signal: Transmission.Signal) {
        signalBroadcast.producer.trySend(signal)
    }

    val effectStream: Flow<EffectWrapper> = effectBroadcast.output

    private val dataBroadcast = testScope.createBroadcast<Transmission.Data>()
    val dataStream = dataBroadcast.output

    private val outGoingQueryChannel: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResultChannel: Channel<QueryResult<Transmission.Data>> =
        Channel(capacity = Channel.BUFFERED)

    private val incomingQueryResponse = queryResultChannel.receiveAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    init {
        initialize()
    }

    fun sendEffect(effect: Transmission.Effect) {
        effectBroadcast.producer.trySend(EffectWrapper(effect))
    }


    fun clear() {
        testScope.cancel()
    }

    private fun initialize() {
        testScope.launch {
            launch {
                outGoingQueryChannel.receiveAsFlow().collect { processQuery(it) }
            }
            transformer.run {
                startSignalCollection(signalBroadcast.output)
                startDataPublishing(dataBroadcast.producer)
                startEffectProcessing(
                    producer = effectBroadcast.producer,
                    incoming = effectBroadcast.output
                )
                launch {
                    startQueryProcessing(
                        incomingQuery = incomingQueryResponse,
                        outGoingQuery = outGoingQueryChannel
                    )
                }
            }
        }
    }

    private fun processQuery(query: Query) = testScope.launch {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
        }
    }

    private fun processDataQuery(
        query: Query.Data
    ) = testScope.launch {
        val dataToSend = QueryResult.Data(
            owner = query.sender,
            data = registry.dataMap[query.key],
            key = query.key
        )
        testScope.launch {
            queryResultChannel.trySend(dataToSend)
        }
    }

    private fun processComputationQuery(
        query: Query.Computation
    ) = testScope.launch {
        val computationToSend = QueryResult.Computation(
            owner = query.sender,
            data = registry.computationMap[query.key],
            key = query.key
        )
        queryResultChannel.trySend(computationToSend)
    }

}

package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher

interface TransformerTestScope<D : Transmission.Data, E : Transmission.Effect> {
    val effectStream: Flow<E>
    val dataStream: SharedFlow<D>
}

internal class TransformerTestScopeImpl<D : Transmission.Data, E : Transmission.Effect, T : Transformer<D, E>>(
    private val registry: RegistryScopeImpl<D, E, T>,
    private val transformer: Transformer<D, E>
) : TransformerTestScope<D, E> {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testScope = CoroutineScope(UnconfinedTestDispatcher())

    private val _incomingSignals = Channel<Transmission.Signal>(capacity = Channel.BUFFERED)

    fun acceptSignal(signal: Transmission.Signal) {
        _incomingSignals.trySend(signal)
    }

    private val incomingEffects =
        Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.BUFFERED)

    private val outGoingEffects =
        Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.BUFFERED)

    override val effectStream: Flow<E> = outGoingEffects.consumeAsFlow().map { it.effect }

    private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

    override val dataStream = outGoingDataChannel.receiveAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    private val outGoingQueryChannel: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResponseChannel: Channel<QueryResponse<D>> =
        Channel(capacity = Channel.BUFFERED)

    private val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    fun acceptEffect(effect: E) {
        incomingEffects.trySend(EffectWrapper(effect))
    }

    init {
        initialize()
    }

    fun clear() {
        testScope.cancel()
    }

    private fun initialize() {
        testScope.launch {
            launch {
                outGoingQueryChannel.consumeAsFlow().collect { processQuery(it) }
            }
            launch {
                transformer.initialize(
                    incomingSignal = _incomingSignals.receiveAsFlow()
                        .shareIn(testScope, SharingStarted.Lazily),
                    incomingEffect = incomingEffects.receiveAsFlow()
                        .shareIn(testScope, SharingStarted.Lazily),
                    outGoingData = outGoingDataChannel,
                    outGoingEffect = outGoingEffects,
                    outGoingQuery = outGoingQueryChannel,
                    incomingQueryResponse = incomingQueryResponse
                )
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
        val dataKey = query.dataOwner.orEmpty() + query.type
        val dataToSend = QueryResponse.Data(
            owner = query.sender,
            data = registry.dataMap[dataKey],
            type = query.type
        )
        queryResponseChannel.trySend(dataToSend)
    }

    private fun processComputationQuery(
        query: Query.Computation
    ) = testScope.launch {
        val computationKey = query.computationOwner.orEmpty() + query.type
        val computationToSend = QueryResponse.Computation(
            owner = query.sender,
            data = registry.computationMap[computationKey],
            type = query.type
        )

        testScope.launch {
            queryResponseChannel.trySend(computationToSend)
        }
    }

}

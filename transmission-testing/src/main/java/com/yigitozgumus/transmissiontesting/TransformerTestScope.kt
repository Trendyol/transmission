package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

interface TransformerTestScope<D : Transmission.Data, E : Transmission.Effect> {
    val effectStream: Flow<E>
    val dataStream: SharedFlow<D>
    fun sendSignal(signal: Transmission.Signal)
    fun sendEffect(effect: E)
}

internal class TransformerTestScopeImpl<D : Transmission.Data, E : Transmission.Effect, T : Transformer<D, E>>(
    private val dispatcher: CoroutineDispatcher,
    private val registry: RegistryScopeImpl<D, E, T>,
    private val transformer: Transformer<D, E>
) : TransformerTestScope<D, E> {

    private val testScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _incomingSignals = Channel<Transmission.Signal>(capacity = Channel.UNLIMITED)

    private val sharedIncomingSignals = _incomingSignals.receiveAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    private val effectChannel =
        Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.BUFFERED)

    private val sharedIncomingEffects = effectChannel.consumeAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    override val effectStream: Flow<E> = sharedIncomingEffects.map { it.effect }

    private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

    override val dataStream = outGoingDataChannel.consumeAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    private val outGoingQueryChannel: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResponseChannel: Channel<QueryResponse<D>> =
        Channel(capacity = Channel.BUFFERED)

    private val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
        .shareIn(testScope, SharingStarted.Lazily)

    override fun sendSignal(signal: Transmission.Signal) {
        _incomingSignals.trySend(signal)
    }

    override fun sendEffect(effect: E) {
        effectChannel.trySend(EffectWrapper(effect))
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
                    incomingSignal = sharedIncomingSignals,
                    incomingEffect = sharedIncomingEffects,
                    outGoingData = outGoingDataChannel,
                    outGoingEffect = effectChannel,
                    outGoingQuery = outGoingQueryChannel,
                    incomingQueryResponse = incomingQueryResponse
                )
            }
        }
    }

    private fun processQuery(query: Query) = testScope.launch(dispatcher) {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
        }
    }

    private fun processDataQuery(
        query: Query.Data
    ) = testScope.launch(dispatcher) {
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
    ) = testScope.launch(dispatcher) {
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
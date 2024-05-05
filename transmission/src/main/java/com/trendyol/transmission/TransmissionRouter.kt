package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.HolderState
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResponse
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter<D : Transmission.Data, E : Transmission.Effect>(
    private val transformerSet: Set<Transformer<D, E>>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : QuerySender<D, E> {

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    private val routerName: String = this::class.java.simpleName

    // region signals

    private val _incomingSignals = Channel<Transmission.Signal>(capacity = Channel.UNLIMITED)

    private val sharedIncomingSignals = _incomingSignals.receiveAsFlow()
        .shareIn(coroutineScope, SharingStarted.Lazily)

    fun processSignal(signal: Transmission.Signal) {
        _incomingSignals.trySend(signal)
    }

    // endregion

    // region effects

    private val effectChannel =
        Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.UNLIMITED)

    private val sharedIncomingEffects = effectChannel.receiveAsFlow()
        .shareIn(coroutineScope, SharingStarted.Lazily)

    // endregion

    private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

    // region query

    private val routerQueryResponseChannel: MutableSharedFlow<QueryResponse<D>> =
        MutableSharedFlow()

    private val outGoingQueryChannel: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResponseChannel: Channel<QueryResponse<D>> =
        Channel(capacity = Channel.BUFFERED)

    private val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
        .shareIn(coroutineScope, SharingStarted.Lazily)

    private suspend fun processQuery(query: Query) = withContext(dispatcher) {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
        }
    }

    private suspend fun processDataQuery(query: Query.Data) = withContext(dispatcher) {
        val dataHolder = transformerSet
            .filter { it.transmissionDataHolderState is HolderState.Initialized }
            .filter { if (query.dataOwner != null) query.dataOwner == it.transformerName else true }
            .find {
                (it.transmissionDataHolderState as HolderState.Initialized)
                    .valueSet.contains(query.type)
            }
        val dataToSend = QueryResponse.Data(
            owner = query.sender,
            data = dataHolder?.holderData?.value?.get(query.type)
        )
        if (query.sender == routerName) {
            routerQueryResponseChannel.emit(dataToSend)
        } else {
            queryResponseChannel.trySend(dataToSend)
        }
    }

    private suspend fun processComputationQuery(query: Query.Computation) =
        withContext(dispatcher) {
            val computationHolder = transformerSet
                .filter { query.computationOwner == it.transformerName }
                .find { it.computationMap.containsKey(query.type) }
            val computationToSend = coroutineScope.async {
                QueryResponse.Computation(
                    owner = query.sender,
                    data = computationHolder?.computationMap?.get(query.type)
                        ?.getResult(computationHolder.communicationScope, query.invalidate)
                )
            }
            if (query.sender == routerName) {
                coroutineScope.launch {
                    routerQueryResponseChannel.emit(computationToSend.await())
                }
            } else {
                coroutineScope.launch {
                    queryResponseChannel.trySend(computationToSend.await())
                }
            }
        }

    // endregion

    fun initialize(
        onData: ((D) -> Unit),
        onEffect: (E) -> Unit = {},
    ) {
        require(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        coroutineScope.launch {
            launch { sharedIncomingEffects.collect { onEffect(it.effect) } }
            launch { outGoingQueryChannel.consumeAsFlow().collect { processQuery(it) } }
            launch { outGoingDataChannel.consumeAsFlow().collect { onData(it) } }
            transformerSet.map { transformer ->
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
    }

    fun clear() {
        transformerSet.forEach { it.clear() }
        coroutineScope.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D? {
        outGoingQueryChannel.trySend(
            Query.Data(
                sender = routerName,
                type = type.simpleName.orEmpty()
            )
        )
        return routerQueryResponseChannel.filterIsInstance<QueryResponse.Data<D>>().first().data
    }

    override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryComputation(
        type: KClass<D>,
        owner: KClass<out T>,
        invalidate: Boolean
    ): D? {
        outGoingQueryChannel.trySend(
            Query.Computation(
                sender = routerName,
                computationOwner = owner.simpleName.orEmpty(),
                type = type.simpleName.orEmpty(),
                invalidate = invalidate
            )
        )
        return routerQueryResponseChannel.filterIsInstance<QueryResponse.Computation<D>>()
            .first().data
    }

    override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryData(
        type: KClass<D>,
        owner: KClass<out T>
    ): D? {
        outGoingQueryChannel.trySend(
            Query.Data(
                sender = routerName,
                dataOwner = owner.simpleName.orEmpty(),
                type = type.simpleName.orEmpty()
            )
        )
        return routerQueryResponseChannel.filterIsInstance<QueryResponse.Data<D>>().first().data
    }
}

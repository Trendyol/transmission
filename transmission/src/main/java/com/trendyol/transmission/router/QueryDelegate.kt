package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.HolderState
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResponse
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class QueryDelegate<D : Transmission.Data, E : Transmission.Effect>(
    private val dispatcher: CoroutineDispatcher
) : QuerySender<D, E> {

    private var _routerRef: TransmissionRouter<D, E>? = null
    private val routerRef: TransmissionRouter<D,E>
        get() {
            return _routerRef ?: throw IllegalStateException("router should be registered")
        }

    fun registerRouter(router: TransmissionRouter<D, E>) {
        _routerRef = router
    }

    private val queryScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun clear() {
        queryScope.cancel()
    }

    private val routerQueryResponseChannel: MutableSharedFlow<QueryResponse<D>> =
        MutableSharedFlow()

    val outGoingQueryChannel: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResponseChannel: Channel<QueryResponse<D>> =
        Channel(capacity = Channel.BUFFERED)

    val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            launch { outGoingQueryChannel.consumeAsFlow().collect { processQuery(it) } }
        }
    }

    private suspend fun processQuery(query: Query) = queryScope.launch(dispatcher) {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
        }
    }

    private suspend fun processDataQuery(
        query: Query.Data
    ) = queryScope.launch(dispatcher) {
        val dataHolder = routerRef.transformerSet
            .filter { it.transmissionDataHolderState is HolderState.Initialized }
            .filter { if (query.dataOwner != null) query.dataOwner == it.transformerName else true }
            .find {
                (it.transmissionDataHolderState as HolderState.Initialized)
                    .valueSet.contains(query.type)
            }
        val dataToSend = QueryResponse.Data(
            owner = query.sender,
            data = dataHolder?.holderData?.value?.get(query.type),
            type = query.type
        )
        if (query.sender == routerRef.routerName) {
            routerQueryResponseChannel.emit(dataToSend)
        } else {
            queryResponseChannel.trySend(dataToSend)
        }
    }

    private suspend fun processComputationQuery(
        query: Query.Computation
    ) = queryScope.launch(dispatcher) {
        val computationHolder = routerRef.transformerSet
            .filter { query.computationOwner == it.transformerName }
            .find { it.computationMap.containsKey(query.type) }
        val computationToSend = queryScope.async {
            QueryResponse.Computation(
                owner = query.sender,
                data = computationHolder?.computationMap?.get(query.type)
                    ?.getResult(computationHolder.communicationScope, query.invalidate),
                type = query.type
            )
        }
        if (query.sender == routerRef.routerName) {
            queryScope.launch {
                routerQueryResponseChannel.emit(computationToSend.await())
            }
        } else {
            queryScope.launch {
                queryResponseChannel.trySend(computationToSend.await())
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D? {
        outGoingQueryChannel.trySend(
            Query.Data(
                sender = routerRef.routerName,
                type = type.simpleName.orEmpty()
            )
        )
        return routerQueryResponseChannel
            .filterIsInstance<QueryResponse.Data<D>>()
            .filter { it.type == type.simpleName }
            .first().data
    }

    override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryComputation(
        type: KClass<D>,
        owner: KClass<out T>,
        invalidate: Boolean
    ): D? {
        outGoingQueryChannel.trySend(
            Query.Computation(
                sender = routerRef.routerName,
                computationOwner = owner.simpleName.orEmpty(),
                type = type.simpleName.orEmpty(),
                invalidate = invalidate
            )
        )
        return routerQueryResponseChannel
            .filterIsInstance<QueryResponse.Computation<D>>()
            .filter { it.type == type.simpleName }
            .first().data
    }

    override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryData(
        type: KClass<D>,
        owner: KClass<out T>
    ): D? {
        outGoingQueryChannel.trySend(
            Query.Data(
                sender = routerRef.routerName,
                dataOwner = owner.simpleName.orEmpty(),
                type = type.simpleName.orEmpty()
            )
        )
        return routerQueryResponseChannel
            .filterIsInstance<QueryResponse.Data<D>>()
            .filter { it.type == type.simpleName }
            .first().data
    }
}

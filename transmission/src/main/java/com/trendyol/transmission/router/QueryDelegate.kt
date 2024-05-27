package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
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

internal class QueryDelegate(
    private val dispatcher: CoroutineDispatcher,
    private val routerRef: TransmissionRouter
) : QuerySender {

    private val queryScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun clear() {
        queryScope.cancel()
    }

    private val routerQueryResponseChannel: MutableSharedFlow<QueryResponse<Transmission.Data>> =
        MutableSharedFlow()

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResponseChannel: Channel<QueryResponse<Transmission.Data>> =
        Channel(capacity = Channel.BUFFERED)

    val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            launch { outGoingQuery.consumeAsFlow().collect { processQuery(it) } }
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
            .filter { it.storage.isHolderStateInitialized() }
            .filter { if (query.dataOwner != null) query.dataOwner == it.transformerName else true }
            .find { it.storage.isHolderDataDefined(query.type) }
        val dataToSend = QueryResponse.Data(
            owner = query.sender,
            data = dataHolder?.storage?.getHolderDataByType(query.type),
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
            .find { it.storage.hasComputation(query.type) }
        val computationToSend = queryScope.async {
            QueryResponse.Computation(
                owner = query.sender,
                data = computationHolder?.storage?.getComputationByType(query.type)
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


    override suspend fun <D : Transmission.Data> queryData(
        type: KClass<D>,
        owner: KClass<out Transformer>?
    ): D? {
        outGoingQuery.trySend(
            Query.Data(
                sender = routerRef.routerName,
                dataOwner = owner?.simpleName,
                type = type.simpleName.orEmpty()
            )
        )
        return routerQueryResponseChannel
            .filterIsInstance<QueryResponse.Data<D>>()
            .filter { it.type == type.simpleName }
            .first().data
    }

    override suspend fun <D : Transmission.Data, T : Transformer> queryComputation(
        type: KClass<D>,
        owner: KClass<out T>,
        invalidate: Boolean
    ): D? {
        outGoingQuery.trySend(
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
}

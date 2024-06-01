package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResult
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
    private val queryScope: CoroutineScope,
    private val routerRef: TransmissionRouter
) : QuerySender {

    private val routerQueryResultChannel: MutableSharedFlow<QueryResult<Transmission.Data>> =
        MutableSharedFlow()

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResultChannel: Channel<QueryResult<Transmission.Data>> =
        Channel(capacity = Channel.BUFFERED)

    val incomingQueryResponse = queryResultChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            launch { outGoingQuery.consumeAsFlow().collect { processQuery(it) } }
        }
    }

    private suspend fun processQuery(query: Query) = queryScope.launch {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
        }
    }

    private suspend fun processDataQuery(
        query: Query.Data
    ) = queryScope.launch {
        val dataHolder = routerRef.transformerSet
            .filter { it.storage.isHolderStateInitialized() }
            .filter { if (query.dataOwner != null) query.dataOwner == it.identifier else true }
            .find { it.storage.isHolderDataDefined(query.type) }
        val dataToSend = QueryResult.Data(
            owner = query.sender,
            data = dataHolder?.storage?.getHolderDataByType(query.type),
            type = query.type
        )
        if (query.sender == routerRef.routerName) {
            routerQueryResultChannel.emit(dataToSend)
        } else {
            queryResultChannel.trySend(dataToSend)
        }
    }

    private suspend fun processComputationQuery(
        query: Query.Computation
    ) = queryScope.launch {
        val computationHolder = routerRef.transformerSet
            .filter { query.computationOwner == it.identifier }
            .find { it.storage.hasComputation(query.type) }
        val computationToSend = queryScope.async {
            QueryResult.Computation(
                owner = query.sender,
                data = computationHolder?.storage?.getComputationByType(query.type)
                    ?.getResult(computationHolder.communicationScope, query.invalidate),
                type = query.type
            )
        }
        if (query.sender == routerRef.routerName) {
            queryScope.launch {
                routerQueryResultChannel.emit(computationToSend.await())
            }
        } else {
            queryScope.launch {
                queryResultChannel.trySend(computationToSend.await())
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
        return routerQueryResultChannel
            .filterIsInstance<QueryResult.Data<D>>()
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
        return routerQueryResultChannel
            .filterIsInstance<QueryResult.Computation<D>>()
            .filter { it.type == type.simpleName }
            .first().data
    }
}

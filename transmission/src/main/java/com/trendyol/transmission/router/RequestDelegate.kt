package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.Query
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.RequestHandler
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

internal class RequestDelegate(
    private val queryScope: CoroutineScope,
    private val routerRef: TransmissionRouter
) : RequestHandler {

    private val routerQueryResultChannel: MutableSharedFlow<QueryResult> = MutableSharedFlow()

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResultChannel: Channel<QueryResult> = Channel(capacity = Channel.BUFFERED)

    val incomingQueryResponse = queryResultChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            outGoingQuery.consumeAsFlow().collect { processQuery(it) }
        }
    }

    private suspend fun processQuery(query: Query) = queryScope.launch {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
            is Query.ComputationWithArgs<*> -> processComputationQueryWithArgs(query)
            is Query.Execution -> processExecution(query)
            is Query.ExecutionWithArgs<*> -> processExecutionWithArgs(query)
        }
    }

    private suspend fun processDataQuery(
        query: Query.Data
    ) = queryScope.launch {
        val dataHolder = routerRef.transformerSet
            .filter { it.storage.isHolderStateInitialized() }
            .find { it.storage.isHolderDataDefined(query.key) }
        val dataToSend = QueryResult.Data(
            owner = query.sender,
            key = query.key,
            data = dataHolder?.storage?.getHolderDataByKey(query.key),
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
            .find { it.storage.hasComputation(query.key) }
        val computationToSend = queryScope.async {
            val computationData = runCatching {
                computationHolder?.storage?.getComputationByKey(query.key)
                    ?.getResult(computationHolder.communicationScope, query.invalidate)
            }.onFailure{
                computationHolder?.onError(it)
            }.getOrNull()

            QueryResult.Computation(
                owner = query.sender,
                key = query.key,
                data = computationData,
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

    private suspend fun <A : Any> processComputationQueryWithArgs(
        query: Query.ComputationWithArgs<A>
    ) = queryScope.launch {
        val computationHolder = routerRef.transformerSet
            .find { it.storage.hasComputation(query.key) }
        val computationToSend = queryScope.async {
            val computationData = runCatching {
                computationHolder?.storage?.getComputationByKey<A>(query.key)
                    ?.getResult(
                        computationHolder.communicationScope,
                        query.invalidate,
                        query.args
                    )
            }.onFailure{
                computationHolder?.onError(it)
            }.getOrNull()

            QueryResult.Computation(
                owner = query.sender,
                key = query.key,
                data = computationData,
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

    private fun processExecution(
        query: Query.Execution
    ) = queryScope.launch {
        val executionHolder = routerRef.transformerSet
            .find { it.storage.hasExecution(query.key) } ?: return@launch
        runCatching {
            executionHolder.storage.getExecutionByKey(query.key)
                ?.execute(executionHolder.communicationScope)
        }.onFailure(executionHolder::onError).getOrNull()
    }

    private fun <A : Any> processExecutionWithArgs(query: Query.ExecutionWithArgs<A>) =
        queryScope.launch {
            val executionHolder = routerRef.transformerSet
                .find { it.storage.hasExecution(query.key) } ?: return@launch
            runCatching {
                executionHolder.storage.getExecutionByKey<A>(query.key)
                    ?.execute(executionHolder.communicationScope, query.args)
            }.onFailure(executionHolder::onError).getOrNull()
        }

    override suspend fun <C : Contract.Data<D>, D : Transmission.Data> getData(contract: C): D? {
        outGoingQuery.trySend(
            Query.Data(sender = routerRef.routerName, key = contract.key)
        )
        return routerQueryResultChannel
            .filterIsInstance<QueryResult.Data<D>>()
            .filter { it.key == contract.key && it.owner == routerRef.routerName }
            .first().data
    }

    override suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean
    ): D? {
        outGoingQuery.trySend(
            Query.Computation(
                sender = routerRef.routerName,
                key = contract.key,
                invalidate = invalidate
            )
        )
        return routerQueryResultChannel
            .filterIsInstance<QueryResult.Computation<D>>()
            .filter { it.key == contract.key && it.owner == routerRef.routerName }
            .first().data
    }

    override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean
    ): D? {
        outGoingQuery.trySend(
            Query.ComputationWithArgs(
                sender = routerRef.routerName,
                key = contract.key,
                args = args,
                invalidate = invalidate
            )
        )
        return routerQueryResultChannel
            .filterIsInstance<QueryResult.Computation<D>>()
            .filter { it.key == contract.key && it.owner == routerRef.routerName }
            .first().data
    }

    override suspend fun <C : Contract.Execution> execute(contract: C) {
        outGoingQuery.trySend(
            Query.Execution(
                sender = routerRef.routerName,
                key = contract.key,
            )
        )
    }

    override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
        contract: C,
        args: A
    ) {
        outGoingQuery.trySend(
            Query.ExecutionWithArgs(
                sender = routerRef.routerName,
                key = contract.key,
                args = args,
            )
        )
    }
}

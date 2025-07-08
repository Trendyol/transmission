package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.QueryType
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

internal class QueryManager(
    private val queryScope: CoroutineScope,
    private val routerRef: TransmissionRouter,
    private val capacity: Capacity = Capacity.Default,
) {

    private val routerQueryResultChannel: MutableSharedFlow<QueryResult> = MutableSharedFlow()

    val outGoingQuery: Channel<QueryType> = Channel(capacity = capacity.value)
    private val queryResultChannel: Channel<QueryResult> = Channel(capacity = capacity.value)

    val incomingQueryResponse = queryResultChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            outGoingQuery.consumeAsFlow().collect { processQuery(it) }
        }
    }

    val handler = object : QueryHandler {

        override suspend fun <D : Transmission.Data?> getData(contract: Contract.DataHolder<D>): D {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                QueryType.Data(
                    sender = routerRef.routerName,
                    key = contract.key,
                    queryIdentifier = queryIdentifier
                )
            )
            return routerQueryResultChannel
                .filterIsInstance<QueryResult.Data<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun <D : Any?> compute(
            contract: Contract.Computation<D>,
            invalidate: Boolean,
        ): D {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                QueryType.Computation(
                    sender = routerRef.routerName,
                    key = contract.key,
                    invalidate = invalidate,
                    queryIdentifier = queryIdentifier
                )
            )
            return routerQueryResultChannel
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun <A : Any, D : Any?> compute(
            contract: Contract.ComputationWithArgs<A, D>,
            args: A,
            invalidate: Boolean,
        ): D {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                QueryType.ComputationWithArgs(
                    sender = routerRef.routerName,
                    key = contract.key,
                    args = args,
                    invalidate = invalidate,
                    queryIdentifier = queryIdentifier
                )
            )
            return routerQueryResultChannel
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun execute(contract: Contract.Execution) {
            outGoingQuery.send(
                QueryType.Execution(
                    key = contract.key,
                )
            )
        }

        override suspend fun <A : Any> execute(
            contract: Contract.ExecutionWithArgs<A>,
            args: A,
        ) {
            outGoingQuery.send(
                QueryType.ExecutionWithArgs(
                    key = contract.key,
                    args = args,
                )
            )
        }
    }

    // region process queries

    private fun processQuery(query: QueryType) = queryScope.launch {
        when (query) {
            is QueryType.Computation -> processComputationQuery(query)
            is QueryType.Data -> processDataQuery(query)
            is QueryType.ComputationWithArgs<*> -> processComputationQueryWithArgs(query)
            is QueryType.Execution -> processExecution(query)
            is QueryType.ExecutionWithArgs<*> -> processExecutionWithArgs(query)
        }
    }

    private fun processDataQuery(
        query: QueryType.Data,
    ) = queryScope.launch {
        val dataHolder = routerRef.transformerSet
            .filter { it.storage.isHolderStateInitialized() }
            .find { it.storage.isHolderDataDefined(query.key) }
        val dataToSend = QueryResult.Data(
            owner = query.sender,
            key = query.key,
            data = dataHolder?.storage?.getHolderDataByKey(query.key),
            resultIdentifier = query.queryIdentifier,
        )
        if (query.sender == routerRef.routerName) {
            routerQueryResultChannel.emit(dataToSend)
        } else {
            queryResultChannel.send(dataToSend)
        }
    }

    private fun processComputationQuery(
        query: QueryType.Computation,
    ) = queryScope.launch {
        val computationHolder = routerRef.transformerSet
            .find { it.storage.hasComputation(query.key) }
        val computationToSend = queryScope.async {
            val computationData = runCatching {
                computationHolder?.storage?.getComputationByKey(query.key)
                    ?.getResult(computationHolder.communicationScope, query.invalidate)
            }.onFailure {
                computationHolder?.onError(it)
            }.getOrNull()

            QueryResult.Computation(
                owner = query.sender,
                key = query.key,
                data = computationData,
                resultIdentifier = query.queryIdentifier
            )
        }
        if (query.sender == routerRef.routerName) {
            routerQueryResultChannel.emit(computationToSend.await())
        } else {
            queryResultChannel.send(computationToSend.await())
        }
    }

    private fun <A : Any> processComputationQueryWithArgs(
        query: QueryType.ComputationWithArgs<A>,
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
            }.onFailure {
                computationHolder?.onError(it)
            }.getOrNull()

            QueryResult.Computation(
                owner = query.sender,
                key = query.key,
                data = computationData,
                resultIdentifier = query.queryIdentifier
            )
        }
        if (query.sender == routerRef.routerName) {
            routerQueryResultChannel.emit(computationToSend.await())
        } else {
            queryResultChannel.send(computationToSend.await())
        }
    }

    private fun processExecution(
        query: QueryType.Execution,
    ) = queryScope.launch {
        val executionHolder = routerRef.transformerSet
            .find { it.storage.hasExecution(query.key) } ?: return@launch
        runCatching {
            executionHolder.storage.getExecutionByKey(query.key)
                ?.execute(executionHolder.communicationScope)
        }.onFailure(executionHolder::onError).getOrNull()
    }

    private fun <A : Any> processExecutionWithArgs(query: QueryType.ExecutionWithArgs<A>) =
        queryScope.launch {
            val executionHolder = routerRef.transformerSet
                .find { it.storage.hasExecution(query.key) } ?: return@launch
            runCatching {
                executionHolder.storage.getExecutionByKey<A>(query.key)
                    ?.execute(executionHolder.communicationScope, query.args)
            }.onFailure(executionHolder::onError).getOrNull()
        }

    // endregion
}

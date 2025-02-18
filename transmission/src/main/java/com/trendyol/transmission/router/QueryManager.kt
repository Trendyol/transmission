package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler
import com.trendyol.transmission.transformer.request.QueryResult
import com.trendyol.transmission.transformer.request.QueryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class QueryManager(
    private val queryScope: CoroutineScope,
    private val routerRef: TransmissionRouter,
) {

    private val capacity = Capacity.Default

    private val routerQueryResultChannel =
        MutableSharedFlow<QueryResult>(extraBufferCapacity = capacity.value)
    val outGoingQuery = MutableSharedFlow<QueryType>(extraBufferCapacity = capacity.value)

    private val queryResultChannel =
        MutableSharedFlow<QueryResult>(extraBufferCapacity = capacity.value)
    val incomingQueryResponse = queryResultChannel.asSharedFlow()

    init {
        queryScope.launch {
            outGoingQuery.collect { processQuery(it) }
        }
    }

    val handler = object : QueryHandler {

        override suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.emit(
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

        override suspend fun <C : Contract.Computation<D>, D : Any> compute(
            contract: C,
            invalidate: Boolean,
        ): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.emit(
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

        override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
            contract: C,
            args: A,
            invalidate: Boolean,
        ): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.emit(
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
            outGoingQuery.emit(
                QueryType.Execution(
                    key = contract.key,
                )
            )
        }

        override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
            contract: C,
            args: A,
        ) {
            outGoingQuery.emit(
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
            queryResultChannel.emit(dataToSend)
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
            queryResultChannel.emit(computationToSend.await())
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
            queryResultChannel.emit(computationToSend.await())
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

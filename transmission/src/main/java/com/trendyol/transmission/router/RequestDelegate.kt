package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
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
    private val routerRef: TransmissionRouter,
    private val registry: RegistryScopeImpl? = null,
) : RequestHandler {

    private val routerQueryResultChannel: MutableSharedFlow<QueryResult> = MutableSharedFlow()

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)

    private val queryResultChannel: Channel<QueryResult> = Channel(capacity = Channel.BUFFERED)

    val incomingQueryResponse = queryResultChannel.receiveAsFlow()
        .shareIn(queryScope, SharingStarted.Lazily)

    init {
        queryScope.launch {
            if (registry != null) {
                outGoingQuery.consumeAsFlow().collect { testQuery(it) }
            } else {
                outGoingQuery.consumeAsFlow().collect { processQuery(it) }
            }
        }
    }

    // region process queries

    private fun processQuery(query: Query) = queryScope.launch {
        when (query) {
            is Query.Computation -> processComputationQuery(query)
            is Query.Data -> processDataQuery(query)
            is Query.ComputationWithArgs<*> -> processComputationQueryWithArgs(query)
            is Query.Execution -> processExecution(query)
            is Query.ExecutionWithArgs<*> -> processExecutionWithArgs(query)
            is Query.Checkpoint<*> -> processCheckpoint(query)
        }
    }

    private fun <A : Any> processCheckpoint(query: Query.Checkpoint<A>) = queryScope.launch {
        queryResultChannel.send(
            QueryResult.Checkpoint(
                owner = query.sender,
                key = query.key,
                data = query.args,
                resultIdentifier = query.queryIdentifier
            )
        )
    }

    private fun processDataQuery(
        query: Query.Data,
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
        query: Query.Computation,
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
            queryScope.launch {
                routerQueryResultChannel.emit(computationToSend.await())
            }
        } else {
            queryScope.launch {
                queryResultChannel.send(computationToSend.await())
            }
        }
    }

    private fun <A : Any> processComputationQueryWithArgs(
        query: Query.ComputationWithArgs<A>,
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
            queryScope.launch {
                routerQueryResultChannel.emit(computationToSend.await())
            }
        } else {
            queryScope.launch {
                queryResultChannel.send(computationToSend.await())
            }
        }
    }

    private fun processExecution(
        query: Query.Execution,
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

    // endregion

    // region test queries

    private fun testQuery(query: Query) = queryScope.launch {
        when (query) {
            is Query.Computation -> testComputationQuery(query)
            is Query.Data -> testDataQuery(query)
            is Query.ComputationWithArgs<*> -> testComputationQueryWithArgs(query)
            is Query.Execution -> {}
            is Query.ExecutionWithArgs<*> -> {}
            is Query.Checkpoint<*> -> {}
        }
    }

    private fun testDataQuery(
        query: Query.Data,
    ) = queryScope.launch {
        val dataToSend = QueryResult.Data(
            owner = query.sender,
            data = registry?.dataMap?.get(query.key),
            key = query.key,
            resultIdentifier = query.queryIdentifier
        )
        queryScope.launch {
            queryResultChannel.trySend(dataToSend)
        }
    }

    private fun testComputationQuery(
        query: Query.Computation,
    ) = queryScope.launch {
        val computationToSend = QueryResult.Computation(
            owner = query.sender,
            data = registry?.computationMap?.get(query.key),
            key = query.key,
            resultIdentifier = query.queryIdentifier
        )
        queryResultChannel.trySend(computationToSend)
    }

    private fun <A : Any> testComputationQueryWithArgs(
        query: Query.ComputationWithArgs<A>,
    ) = queryScope.launch {
        val computationToSend = QueryResult.Computation(
            owner = query.sender,
            data = registry?.computationMap?.get(query.key),
            key = query.key,
            resultIdentifier = query.queryIdentifier
        )
        queryResultChannel.trySend(computationToSend)
    }

    // region Request Handler

    override suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D? {
        val queryIdentifier = IdentifierGenerator.generateIdentifier()
        outGoingQuery.send(
            Query.Data(
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
        outGoingQuery.send(
            Query.Computation(
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
        outGoingQuery.send(
            Query.ComputationWithArgs(
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
            Query.Execution(
                key = contract.key,
            )
        )
    }

    override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
        contract: C,
        args: A,
    ) {
        outGoingQuery.send(
            Query.ExecutionWithArgs(
                key = contract.key,
                args = args,
            )
        )
    }

    // endregion
}

package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.createBroadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

internal class TransformerQueryDelegate(scope: CoroutineScope, identifier: String) {

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    val resultBroadcast = scope.createBroadcast<QueryResult<Transmission.Data>>()

    val interactor: QuerySender = object : QuerySender {

        override suspend fun <C : Contract.Data<D>, D : Transmission.Data> queryData(contract: C): D? {
            outGoingQuery.trySend(Query.Data(sender = identifier, key = contract.key))
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Data<D>>()
                .filter { it.key == contract.key && it.owner == identifier }
                .first().data
        }

        override suspend fun <C : Contract.Computation<D>, D : Transmission.Data> queryComputation(
            contract: C,
            invalidate: Boolean
        ): D? {
            outGoingQuery.trySend(
                Query.Computation(
                    sender = identifier,
                    key = contract.key,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == contract.key && it.owner == identifier }
                .first().data
        }

        override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Transmission.Data> queryComputationWithArgs(
            contract: C,
            args: A,
            invalidate: Boolean
        ): D? {
            outGoingQuery.trySend(
                Query.ComputationWithArgs(
                    sender = identifier,
                    key = contract.key,
                    args = args,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == contract.key && it.owner == identifier }
                .first().data
        }
    }
}

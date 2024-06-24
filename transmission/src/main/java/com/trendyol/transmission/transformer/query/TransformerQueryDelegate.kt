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
        override suspend fun <D : Transmission.Data> queryData(key: String): D? {
            outGoingQuery.trySend(Query.Data(sender = identifier, key = key))
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Data<D>>()
                .filter { it.key == key && it.owner == identifier }
                .first().data
        }

        override suspend fun <D : Transmission.Data> queryComputation(
            key: String,
            invalidate: Boolean
        ): D? {
            outGoingQuery.trySend(
                Query.Computation(
                    sender = identifier,
                    key = key,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == key && it.owner == identifier }
                .first().data
        }

        override suspend fun <A : Any, D : Transmission.Data> queryComputationWithArgs(
            args: A,
            key: String,
            invalidate: Boolean
        ): D? {
            outGoingQuery.trySend(
                Query.ComputationWithArgs(
                    sender = identifier,
                    key = key,
                    args = args,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == key && it.owner == identifier }
                .first().data
        }
    }
}

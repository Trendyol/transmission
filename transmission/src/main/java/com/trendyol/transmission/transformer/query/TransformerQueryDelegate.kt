package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlin.reflect.KClass

internal class TransformerQueryDelegate(scope: CoroutineScope, identifier: String) {

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    val resultBroadcast = scope.createBroadcast<QueryResult<Transmission.Data>>()

    val interactor: QuerySender = object: QuerySender {
        override suspend fun <D : Transmission.Data> queryData(
            type: KClass<D>,
            owner: KClass<out Transformer>?
        ): D? {
            outGoingQuery.trySend(
                Query.Data(
                    sender = identifier,
                    dataOwner = owner?.simpleName,
                    type = type.simpleName.orEmpty()
                )
            )
            return  resultBroadcast.output
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
                    sender = identifier,
                    computationOwner = owner.simpleName.orEmpty(),
                    type = type.simpleName.orEmpty(),
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.type == type.simpleName }
                .first().data
        }

    }
}

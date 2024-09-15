package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.createBroadcast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

internal class TransformerRequestDelegate(scope: CoroutineScope, identity: Contract.Identity) {

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    val resultBroadcast = scope.createBroadcast<QueryResult>()

    val interactor: RequestHandler = object : RequestHandler {

        override suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D? {
            outGoingQuery.send(Query.Data(sender = identity.key, key = contract.key))
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Data<D>>()
                .filter { it.key == contract.key && it.owner == identity.key }
                .first().data
        }

        override suspend fun <C : Contract.Computation<D>, D : Any> compute(
            contract: C,
            invalidate: Boolean
        ): D? {
            outGoingQuery.send(
                Query.Computation(
                    sender = identity.key,
                    key = contract.key,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == contract.key && it.owner == identity.key }
                .first().data
        }

        override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
            contract: C,
            args: A,
            invalidate: Boolean
        ): D? {
            outGoingQuery.send(
                Query.ComputationWithArgs(
                    sender = identity.key,
                    key = contract.key,
                    args = args,
                    invalidate = invalidate
                )
            )
            return resultBroadcast.output
                .filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.key == contract.key && it.owner == identity.key }
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
            args: A
        ) {
            outGoingQuery.send(
                Query.ExecutionWithArgs(
                    key = contract.key,
                    args = args,
                )
            )
        }
    }
}

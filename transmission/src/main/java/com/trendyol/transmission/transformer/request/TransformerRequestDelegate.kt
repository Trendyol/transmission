package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.InternalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.checkpoint.CheckpointHandler
import com.trendyol.transmission.transformer.checkpoint.CheckpointTracker
import com.trendyol.transmission.transformer.checkpoint.CheckpointValidator
import com.trendyol.transmission.transformer.handler.CommunicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

@OptIn(InternalTransmissionApi::class)
@ExperimentalTransmissionApi
internal class TransformerRequestDelegate(
    scope: CoroutineScope,
    checkpointTrackerProvider: () -> CheckpointTracker?,
    identity: Contract.Identity
) {

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    val resultBroadcast = scope.createBroadcast<QueryResult>()

    val checkpointHandler: CheckpointHandler by lazy {
        object : CheckpointHandler {

            @ExperimentalTransmissionApi
            override suspend fun CommunicationScope.pauseOn(contract: Contract.Checkpoint.Default) {
                val queryIdentifier = IdentifierGenerator.generateIdentifier()
                suspendCancellableCoroutine<Unit> { continuation ->
                    val validator =
                        object : CheckpointValidator<Contract.Checkpoint.Default, Unit> {

                            override suspend fun validate(
                                contract: Contract.Checkpoint.Default,
                                args: Unit
                            ): Boolean {
                                continuation.resume(Unit)
                                return true
                            }
                        }
                    checkpointTrackerProvider()?.run {
                        registerContract(contract, queryIdentifier)
                        putOrCreate(queryIdentifier, validator)
                    }
                }
            }

            @ExperimentalTransmissionApi
            override suspend fun CommunicationScope.pauseOn(
                vararg contract: Contract.Checkpoint.Default
            ) {
                val contractList = contract.toList()
                check(contractList.isNotEmpty()) {
                    "At least one checkpoint should be provided"
                }
                check(contractList.toSet().size == contractList.size) {
                    "All Checkpoint Contracts should be unique"
                }
                val queryIdentifier = IdentifierGenerator.generateIdentifier()
                suspendCancellableCoroutine<Unit> { continuation ->
                    val validator =
                        object : CheckpointValidator<Contract.Checkpoint.Default, Unit> {
                            private val lock = Mutex()
                            private val contractMap =
                                ConcurrentHashMap<Contract.Checkpoint.Default, Boolean>()
                                    .apply { putAll(contractList.map { it to false }) }

                            override suspend fun validate(
                                contract: Contract.Checkpoint.Default,
                                args: Unit
                            ): Boolean {
                                lock.withLock { contractMap.put(contract, true) }
                                if (contractMap.values.all { it }) {
                                    continuation.resume(Unit)
                                    return true
                                } else return false
                            }
                        }
                    checkpointTrackerProvider()?.run {
                        contractList.forEach { registerContract(it, queryIdentifier) }
                        putOrCreate(queryIdentifier, validator)
                    }
                }
            }

            @ExperimentalTransmissionApi
            override suspend fun <C : Contract.Checkpoint.WithArgs<A>, A : Any> CommunicationScope.pauseOn(
                contract: C
            ): A {
                val queryIdentifier = IdentifierGenerator.generateIdentifier()
                return suspendCancellableCoroutine<A> { continuation ->
                    val validator = object : CheckpointValidator<C, A> {
                        override suspend fun validate(contract: C, args: A): Boolean {
                            continuation.resume(args)
                            return true
                        }
                    }
                    checkpointTrackerProvider()?.run {
                        registerContract(contract, queryIdentifier)
                        putOrCreate(queryIdentifier, validator)
                    }
                }
            }

            override suspend fun validate(contract: Contract.Checkpoint.Default) {
                val validator = checkpointTrackerProvider()
                    ?.useValidator<Contract.Checkpoint.Default, Unit>(contract)
                if (validator?.validate(contract, Unit) == true) {
                    checkpointTrackerProvider()
                        ?.removeValidator(contract)
                }
            }

            override suspend fun <C : Contract.Checkpoint.WithArgs<A>, A : Any> validate(
                contract: C,
                args: A
            ) {
                val validator = checkpointTrackerProvider()
                    ?.useValidator<C, A>(contract)
                if (validator?.validate(contract, args) == true) {
                    checkpointTrackerProvider()
                        ?.removeValidator(contract)
                }
            }
        }
    }

    val requestHandler: RequestHandler = object : RequestHandler {

        override suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                Query.Data(
                    sender = identity.key,
                    key = contract.key,
                    queryIdentifier = queryIdentifier
                )
            )
            return resultBroadcast.output.filterIsInstance<QueryResult.Data<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun <C : Contract.Computation<D>, D : Any> compute(
            contract: C, invalidate: Boolean
        ): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                Query.Computation(
                    sender = identity.key,
                    key = contract.key,
                    invalidate = invalidate,
                    queryIdentifier = queryIdentifier
                )
            )
            return resultBroadcast.output.filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
            contract: C, args: A, invalidate: Boolean
        ): D? {
            val queryIdentifier = IdentifierGenerator.generateIdentifier()
            outGoingQuery.send(
                Query.ComputationWithArgs(
                    sender = identity.key,
                    key = contract.key,
                    args = args,
                    invalidate = invalidate,
                    queryIdentifier = queryIdentifier
                )
            )
            return resultBroadcast.output.filterIsInstance<QueryResult.Computation<D>>()
                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                .first().data
        }

        override suspend fun execute(contract: Contract.Execution) {
            outGoingQuery.send(
                Query.Execution(key = contract.key)
            )
        }

        override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
            contract: C, args: A
        ) {
            outGoingQuery.send(
                Query.ExecutionWithArgs(key = contract.key, args = args)
            )
        }
    }
}

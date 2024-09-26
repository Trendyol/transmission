package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.checkpoint.CheckpointHandler
import com.trendyol.transmission.transformer.checkpoint.CheckpointTracker
import com.trendyol.transmission.transformer.checkpoint.Frequency
import com.trendyol.transmission.transformer.handler.CommunicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@ExperimentalTransmissionApi
internal class TransformerRequestDelegate(
    scope: CoroutineScope,
    checkpointTrackerProvider: () -> CheckpointTracker?,
    identity: Contract.Identity
) {

    val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    val resultBroadcast = scope.createBroadcast<QueryResult>()

    private val frequencyTracker: MutableSet<Contract> = mutableSetOf()
    private val arbitraryFrequencyTracker: MutableSet<Set<Contract>> = mutableSetOf()
    private val frequencyWithArgsTracker: ConcurrentMap<Contract.CheckpointWithArgs<*>, Any> =
        ConcurrentHashMap()

    val checkpointHandler: CheckpointHandler by lazy {
        object : CheckpointHandler {

            override suspend fun CommunicationScope.pauseOn(
                contract: Contract.Checkpoint,
                resumeBlock: suspend CommunicationScope.() -> Unit
            ) {
                when (contract.frequency) {
                    Frequency.Continuous -> {
                        val queryIdentifier = IdentifierGenerator.generateIdentifier()
                        checkpointTrackerProvider()?.putOrCreate(
                            contract = contract,
                            checkpointOwner = identity,
                            identifier = queryIdentifier
                        )
                        resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<Unit>>()
                            .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                            .collect {
                                resumeBlock.invoke(this)
                            }
                    }

                    Frequency.Once -> {
                        if (frequencyTracker.contains(contract)) {
                            resumeBlock.invoke(this)
                        } else {
                            val queryIdentifier = IdentifierGenerator.generateIdentifier()
                            checkpointTrackerProvider()?.putOrCreate(
                                contract = contract,
                                checkpointOwner = identity,
                                identifier = queryIdentifier
                            )
                            frequencyTracker.add(contract)
                            resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<Unit>>()
                                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                                .collect {
                                    resumeBlock.invoke(this)
                                }
                        }
                    }
                }
            }

            @ExperimentalTransmissionApi
            override suspend fun CommunicationScope.pauseOn(
                vararg contract: Contract.Checkpoint,
                resumeBlock: suspend CommunicationScope.() -> Unit
            ) {
                val contractList = contract.toList()
                check(contractList.distinctBy { it.frequency }.size == 1) {
                    "All Checkpoints should have the same frequency"
                }
                check(contractList.isNotEmpty()) {
                    "At least one checkpoint should be provided"
                }
                check(contractList.toSet().size == contractList.size) {
                    "All Checkpoint Contracts should be unique"
                }
                when (contractList.first().frequency) {
                    Frequency.Continuous -> {
                        val queryIdentifier = IdentifierGenerator.generateIdentifier()
                        contractList.forEach { internalContract ->
                            checkpointTrackerProvider()?.putOrCreate(
                                contract = internalContract,
                                checkpointOwner = identity,
                                identifier = queryIdentifier
                            )
                        }
                        resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<Unit>>()
                            .filter { it.resultIdentifier == queryIdentifier }
                            .drop(contractList.size.dec())
                            .collect {
                                resumeBlock.invoke(this)
                            }

                    }

                    Frequency.Once -> {
                        if (arbitraryFrequencyTracker.contains(contractList.toSet())) {
                            resumeBlock.invoke(this)
                        } else {
                            val queryIdentifier = IdentifierGenerator.generateIdentifier()
                            contractList.forEach { internalContract ->
                                checkpointTrackerProvider()?.putOrCreate(
                                    contract = internalContract,
                                    checkpointOwner = identity,
                                    identifier = queryIdentifier
                                )
                            }
                            arbitraryFrequencyTracker.add(contractList.toSet())
                            resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<Unit>>()
                                .filter { it.resultIdentifier == queryIdentifier }
                                .drop(contractList.size.dec())
                                .collect {
                                    resumeBlock.invoke(this)
                                }
                        }
                    }
                }

            }

            override suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> CommunicationScope.pauseOn(
                contract: C,
                resumeBlock: suspend CommunicationScope.(args: A) -> Unit
            ) {
                when (contract.frequency) {
                    Frequency.Continuous -> {
                        val queryIdentifier = IdentifierGenerator.generateIdentifier()
                        checkpointTrackerProvider()?.putOrCreate(
                            contract = contract,
                            checkpointOwner = identity,
                            identifier = queryIdentifier
                        )
                        resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<A>>()
                            .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                            .collect {
                                resumeBlock.invoke(this, it.data)
                            }
                    }

                    Frequency.Once -> {
                        if (frequencyWithArgsTracker.containsKey(contract)) {
                            @Suppress("UNCHECKED_CAST")
                            resumeBlock.invoke(this, frequencyWithArgsTracker[contract] as A)
                        } else {
                            val queryIdentifier = IdentifierGenerator.generateIdentifier()
                            checkpointTrackerProvider()?.putOrCreate(
                                contract = contract,
                                checkpointOwner = identity,
                                identifier = queryIdentifier
                            )
                            resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<A>>()
                                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                                .collect {
                                    frequencyWithArgsTracker[contract] = it.data
                                    resumeBlock.invoke(this, it.data)
                                }
                        }
                    }
                }
            }

            @ExperimentalTransmissionApi
            override suspend fun <C : Contract.CheckpointWithArgs<A>, C2 : Contract.CheckpointWithArgs<B>, A : Any, B : Any> CommunicationScope.pauseOn(
                contract: C,
                contract2: C2,
                resumeBlock: suspend CommunicationScope.(A, B) -> Unit
            ) {
                val contractList = listOf(contract, contract2)
                check(contractList.distinctBy { it.frequency }.size == 1) {
                    "All Checkpoint should have the same frequency"
                }
                check(contractList.toSet().size == contractList.size) {
                    "All Checkpoint Contracts should be unique"
                }
                when (contract.frequency) {
                    Frequency.Continuous -> {
                        val queryIdentifier = IdentifierGenerator.generateIdentifier()
                        listOf(contract, contract2).forEach {
                            checkpointTrackerProvider()?.putOrCreate(
                                contract = it,
                                checkpointOwner = identity,
                                identifier = queryIdentifier
                            )
                        }
                        val flow1 =
                            resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<A>>()
                                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                        val flow2 =
                            resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<B>>()
                                .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                        combine(flow1, flow2) { checkpoint1, checkpoint2 ->
                            resumeBlock.invoke(this, checkpoint1.data, checkpoint2.data)
                        }
                    }

                    Frequency.Once -> {
                        if (frequencyWithArgsTracker.containsKey(contract) && frequencyWithArgsTracker.containsKey(
                                contract2
                            )
                        ) {
                            @Suppress("UNCHECKED_CAST")
                            resumeBlock.invoke(
                                this,
                                frequencyWithArgsTracker[contract] as A,
                                frequencyWithArgsTracker[contract2] as B
                            )
                        } else {
                            val queryIdentifier = IdentifierGenerator.generateIdentifier()
                            listOf(contract, contract2).forEach {
                                checkpointTrackerProvider()?.putOrCreate(
                                    contract = it,
                                    checkpointOwner = identity,
                                    identifier = queryIdentifier
                                )
                            }
                            val flow1 =
                                resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<A>>()
                                    .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                            val flow2 =
                                resultBroadcast.output.filterIsInstance<QueryResult.Checkpoint<B>>()
                                    .filter { it.resultIdentifier == queryIdentifier && it.key == contract.key }
                            combine(flow1, flow2) { checkpoint1, checkpoint2 ->
                                frequencyWithArgsTracker[contract] = checkpoint1.data
                                frequencyWithArgsTracker[contract2] = checkpoint2.data
                                resumeBlock.invoke(this, checkpoint1.data, checkpoint2.data)
                            }
                        }
                    }
                }
            }

            override suspend fun validate(contract: Contract.Checkpoint) {
                val identifier = checkpointTrackerProvider()?.useIdentifier(contract) ?: return
                outGoingQuery.send(
                    Query.Checkpoint(
                        sender = identifier.barrierOwner.key,
                        key = contract.key,
                        args = Unit,
                        queryIdentifier = identifier.value
                    )
                )
            }

            override suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> validate(
                contract: C,
                args: A
            ) {
                val identifier = checkpointTrackerProvider()?.useIdentifier(contract) ?: return
                outGoingQuery.send(
                    Query.Checkpoint(
                        sender = identifier.barrierOwner.key,
                        key = contract.key,
                        args = args,
                        queryIdentifier = identifier.value
                    )
                )
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

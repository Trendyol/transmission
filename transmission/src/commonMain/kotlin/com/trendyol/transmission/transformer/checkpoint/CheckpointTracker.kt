package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.InternalTransmissionApi
import com.trendyol.transmission.transformer.request.Contract
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@OptIn(InternalTransmissionApi::class)
internal class CheckpointTracker {
    private val tracker: ConcurrentMap<String, ArrayDeque<CheckpointValidator<*, *>>> =
        ConcurrentHashMap()
    private val contractTracker: ConcurrentMap<Contract.Checkpoint, String> = ConcurrentHashMap()

    fun registerContract(contract: Contract.Checkpoint, identifier: String) {
        contractTracker.put(contract, identifier)
    }

    fun <C : Contract.Checkpoint, A : Any> putOrCreate(
        identifier: String,
        validator: CheckpointValidator<C, A>
    ) {
        tracker
            .putIfAbsent(identifier, ArrayDeque<CheckpointValidator<*, *>>().apply {
                addLast(validator)
            })?.addLast(validator)
    }

    fun <C : Contract.Checkpoint, A : Any> useValidator(contract: Contract.Checkpoint): CheckpointValidator<C, A>? {
        val identifier = contractTracker[contract] ?: return null
        return tracker[identifier]?.firstOrNull() as? CheckpointValidator<C, A>
    }

    fun removeValidator(contract: Contract.Checkpoint) {
        val identifier = contractTracker[contract] ?: return
        val contractsToRemove = contractTracker.entries
            .filter { it.value == identifier }
            .map { it.key }
        contractsToRemove.forEach {
            contractTracker.remove(it)
        }
        tracker[identifier]?.removeFirstOrNull()
    }
}

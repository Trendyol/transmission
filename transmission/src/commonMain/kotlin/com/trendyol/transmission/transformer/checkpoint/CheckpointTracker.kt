package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.InternalTransmissionApi
import com.trendyol.transmission.transformer.request.Contract

@OptIn(InternalTransmissionApi::class)
internal class CheckpointTracker {
    private val tracker: MutableMap<String, ArrayDeque<CheckpointValidator<*, *>>> = mutableMapOf()
    private val contractTracker: MutableMap<Contract.Checkpoint, String> = mutableMapOf()

    fun registerContract(contract: Contract.Checkpoint, identifier: String) {
        contractTracker.put(contract, identifier)
    }

    fun <C : Contract.Checkpoint, A : Any> putOrCreate(
        identifier: String,
        validator: CheckpointValidator<C, A>
    ) {
        val value = tracker.get(identifier) ?: ArrayDeque<CheckpointValidator<*, *>>()
        value.addLast(validator)
        tracker[identifier] = value
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

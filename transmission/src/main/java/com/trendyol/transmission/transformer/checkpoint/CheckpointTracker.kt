package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.transformer.request.Contract
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class CheckpointTracker {
    private val tracker: ConcurrentMap<Contract, ArrayDeque<IdentifierBundle>> =
        ConcurrentHashMap()

    fun putOrCreate(contract: Contract, barrierOwner: Contract.Identity, identifier: String) {
        tracker
            .putIfAbsent(
                contract,
                ArrayDeque<IdentifierBundle>().apply {
                    addLast(
                        IdentifierBundle(
                            barrierOwner,
                            identifier
                        )
                    )
                })
            ?.addLast(IdentifierBundle(barrierOwner, identifier))
    }

    fun useIdentifier(contract: Contract): IdentifierBundle? {
        return tracker[contract]?.removeFirstOrNull()
    }
}

internal class IdentifierBundle(val barrierOwner: Contract.Identity, val value: String)

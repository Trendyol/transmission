package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried.
 * Can be queried using [QueryHandler.compute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <T : Any?> ComputationScope.register(
    contract: Contract.Computation<T>,
    computation: suspend QueryHandler.() -> T,
) {
    this.computationRegistry.buildWith(contract.key, contract.useCache, computation)
}

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried. This computation accepts any class as Argument.
 * Can be queried using [QueryHandler.compute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <A : Any, T : Any?> ComputationScope.register(
    contract: Contract.ComputationWithArgs<A,T>,
    computation: suspend QueryHandler.(args: A) -> T,
) {
    this.computationRegistry.buildWith(contract.key, contract.useCache, computation)
}

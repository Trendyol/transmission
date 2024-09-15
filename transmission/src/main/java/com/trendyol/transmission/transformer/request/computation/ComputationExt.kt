package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.RequestHandler

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried.
 * Can be queried using [RequestHandler.compute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.Computation<T>, T : Any> ComputationScope.register(
    contract: C,
    computation: suspend RequestHandler.() -> T?,
) {
    this.computationRegistry.buildWith(contract.key, contract.useCache, computation)
}

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried. This computation accepts any class as Argument.
 * Can be queried using [RequestHandler.compute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.ComputationWithArgs<A, T>, A : Any, T : Any> ComputationScope.register(
    contract: C,
    computation: suspend RequestHandler.(args: A) -> T?,
) {
    this.computationRegistry.buildWith(contract.key, contract.useCache, computation)
}

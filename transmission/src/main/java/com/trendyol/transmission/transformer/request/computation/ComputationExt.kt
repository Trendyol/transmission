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
 * Can be queried using [RequestHandler.execute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.Computation<T>, T : Any> ComputationRegistry.registerComputation(
    contract: C,
    computation: suspend RequestHandler.() -> T?,
): ComputationRegistry {
    return this.apply { buildWith(contract.key, contract.useCache, computation) }
}

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried. This computation accepts any class as Argument.
 * Can be queried using [RequestHandler.execute]
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.ComputationWithArgs<A, T>, A : Any, T : Any> ComputationRegistry.registerComputation(
    contract: C,
    computation: suspend RequestHandler.(args: A) -> T?,
): ComputationRegistry {
    return this.apply { buildWith(contract.key, contract.useCache, computation) }
}

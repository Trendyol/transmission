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
 * @param useCache Stores the result after first computation
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.Computation<T>, T : Transmission.Data> Transformer.registerComputation(
    contract: C,
    computation: suspend RequestHandler.() -> T?,
) {
    ComputationBuilder().buildWith(contract.key, contract.useCache, this, computation)
}

/**
 * Throws [IllegalArgumentException] when multiple computations with the same key
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried. This computation accepts any class as Argument.
 * Can be queried using [RequestHandler.execute]
 * @param useCache Stores the result after first computation
 * @param computation Computation to get the result [Transmission.Data]
 */
fun <C : Contract.ComputationWithArgs<A, T>, A : Any, T : Any> Transformer.registerComputation(
    contract: C,
    computation: suspend RequestHandler.(args: A) -> T?,
) {
    ComputationBuilder().buildWith(contract.key, contract.useCache, this, computation)
}

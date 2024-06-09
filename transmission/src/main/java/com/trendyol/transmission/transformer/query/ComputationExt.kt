package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

/**
 * Throws [IllegalArgumentException] when multiple computations with the same return type
 * are defined inside the [Transformer].
 *
 * Adds a computation to [Transformer] to be queried.
 * @param useCache Stores the result after first computation
 * @param computation Computation to get the result [Transmission.Data]
 */
inline fun <reified T : Transmission.Data> Transformer.registerComputation(
    key: String,
    useCache: Boolean = false,
    noinline computation: suspend QuerySender.() -> T?,
) {
    ComputationBuilder<T>().buildWith(key, useCache, this, computation)
}

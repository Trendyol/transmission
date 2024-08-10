package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer

class ComputationScope internal constructor(internal val computationRegistry: ComputationRegistry)

fun Transformer.computations(scope: ComputationScope.() -> Unit): ComputationRegistry {
    val computationRegistry = ComputationRegistry(this)
    ComputationScope(computationRegistry).apply(scope)
    return computationRegistry
}

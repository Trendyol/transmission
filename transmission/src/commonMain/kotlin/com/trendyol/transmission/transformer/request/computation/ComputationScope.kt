package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer

class ComputationScope internal constructor(internal val computationRegistry: ComputationRegistry)

class Computations internal constructor()

fun Transformer.computations(scope: ComputationScope.() -> Unit = {}): Computations {
    this.computationRegistry.clear()
    ComputationScope(computationRegistry).apply(scope)
    return Computations()
}

fun Computations.extendComputations(
    transformer: Transformer,
    scope: ComputationScope.() -> Unit = {}
): Computations {
    ComputationScope(transformer.computationRegistry).apply(scope)
    return Computations()
}

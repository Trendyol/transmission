package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer

class ComputationScope internal constructor(internal val computationRegistry: ComputationRegistry)

class Computations internal constructor()

class ExtendedComputations internal constructor()

fun Transformer.createComputations(scope: ComputationScope.() -> Unit): Computations {
    this.computationRegistry.clear()
    ComputationScope(computationRegistry).apply(scope)
    return Computations()
}

fun Transformer.extendComputations(scope: ComputationScope.() -> Unit): ExtendedComputations {
    ComputationScope(computationRegistry).apply(scope)
    return ExtendedComputations()
}

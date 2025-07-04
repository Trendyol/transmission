package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer

class ComputationScope internal constructor(internal val computationRegistry: ComputationRegistry)

class Computations internal constructor()

fun Transformer.computations(scope: ComputationScope.() -> Unit = {}): Computations {
    this.computationRegistry.clear()
    ComputationScope(computationRegistry).apply(scope)
    return Computations()
}

/**
 * Creates and registers computations for this transformer.
 * 
 * @deprecated Use [computations] instead. This method is kept for binary compatibility.
 * @param scope DSL lambda for defining computations
 * @return A [Computations] instance representing the registered computations
 */
@Deprecated(
    message = "Use computations instead",
    replaceWith = ReplaceWith("computations(scope)"),
    level = DeprecationLevel.WARNING
)
fun Transformer.createComputations(scope: ComputationScope.() -> Unit = {}): Computations {
    return computations(scope)
}

fun Computations.extendComputations(
    transformer: Transformer,
    scope: ComputationScope.() -> Unit = {}
): Computations {
    ComputationScope(transformer.computationRegistry).apply(scope)
    return Computations()
}

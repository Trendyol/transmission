package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.HandlerScope
import com.trendyol.transmission.transformer.handler.UpdateHandlerScope
import com.trendyol.transmission.transformer.handler.extendEffect
import com.trendyol.transmission.transformer.handler.extendSignal
import com.trendyol.transmission.transformer.handler.overrideEffect
import com.trendyol.transmission.transformer.handler.overrideSignal
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler
import com.trendyol.transmission.transformer.request.computation.ComputationScope
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.execution.ExecutionScope
import com.trendyol.transmission.transformer.request.execution.register

/**
 * Adds handlers incrementally to a Transformer without clearing existing handlers.
 * This is useful for defining handlers in the init block or in functions.
 *
 * Example usage:
 * ```
 * class MyTransformer : Transformer() {
 *     private val holder = dataHolder(MyData())
 *
 *     init {
 *         addHandlers {
 *             onSignal<MySignal> { signal ->
 *                 // Handle signal
 *             }
 *             onEffect<MyEffect> { effect ->
 *                 // Handle effect
 *             }
 *         }
 *     }
 * }
 * ```
 */
fun Transformer.addHandlers(scope: HandlerScope.() -> Unit): Transformer {
    HandlerScope(handlerRegistry).apply(scope)
    return this
}

/**
 * Updates existing handlers in a Transformer without clearing them.
 * This allows extending or overriding existing handler implementations.
 *
 * Example usage:
 * ```
 * class MyTransformer : Transformer() {
 *     private val holder = dataHolder(MyData())
 *
 *     init {
 *         // Define initial handlers
 *         handlers { ... }
 *
 *         // Later, extend or override handlers
 *         updateHandlers {
 *             extendEffect<MyEffect> { effect ->
 *                 // Additional handling for the effect
 *             }
 *
 *             overrideSignal<MySignal> { signal ->
 *                 // Completely replace handling for the signal
 *             }
 *         }
 *     }
 * }
 * ```
 */
fun Transformer.updateHandlers(scope: UpdateHandlerScope.() -> Unit): Transformer {
    UpdateHandlerScope(handlerRegistry).apply(scope)
    return this
}

/**
 * Functions for adding computations incrementally to a Transformer.
 * This is useful for defining computations in the init block or in functions.
 *
 * Example usage:
 * ```
 * class MyTransformer : Transformer() {
 *     init {
 *         addComputations {
 *             register(myComputationContract) {
 *                 // Compute something
 *             }
 *         }
 *     }
 * }
 * ```
 */
fun Transformer.addComputations(scope: ComputationScope.() -> Unit): Transformer {
    ComputationScope(computationRegistry).apply(scope)
    return this
}

/**
 * Functions for adding executions incrementally to a Transformer.
 * This is useful for defining executions in the init block or in functions.
 *
 * Example usage:
 * ```
 * class MyTransformer : Transformer() {
 *     init {
 *         addExecutions {
 *             register(myExecutionContract) {
 *                 // Execute something
 *             }
 *         }
 *     }
 * }
 * ```
 */
fun Transformer.addExecutions(scope: ExecutionScope.() -> Unit): Transformer {
    ExecutionScope(executionRegistry).apply(scope)
    return this
}

/**
 * Convenience extension to register a computation directly to a Transformer.
 */
fun <T : Any?> Transformer.registerComputation(
    contract: Contract.Computation<T>,
    computation: suspend QueryHandler.() -> T
): Transformer {
    addComputations {
        register(contract, computation)
    }
    return this
}

/**
 * Convenience extension to register a computation with arguments directly to a Transformer.
 */
fun <A : Any, T : Any?> Transformer.registerComputation(
    contract: Contract.ComputationWithArgs<A, T>,
    computation: suspend QueryHandler.(args: A) -> T
): Transformer {
    addComputations {
        register(contract, computation)
    }
    return this
}

/**
 * Convenience extension to register an execution directly to a Transformer.
 */
fun Transformer.registerExecution(
    contract: Contract.Execution,
    execution: suspend QueryHandler.() -> Unit
): Transformer {
    addExecutions {
        register(contract, execution)
    }
    return this
}

/**
 * Convenience extension to register an execution with arguments directly to a Transformer.
 */
fun <A : Any> Transformer.registerExecution(
    contract: Contract.ExecutionWithArgs<A>,
    execution: suspend QueryHandler.(args: A) -> Unit
): Transformer {
    addExecutions {
        register(contract, execution)
    }
    return this
}

/**
 * Convenience extension to extend an existing effect handler in a Transformer.
 */
inline fun <reified T : Transmission.Effect> Transformer.extendEffectHandler(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
): Transformer {
    updateHandlers {
        extendEffect(lambda)
    }
    return this
}

/**
 * Convenience extension to override an existing effect handler in a Transformer.
 */
inline fun <reified T : Transmission.Effect> Transformer.overrideEffectHandler(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
): Transformer {
    updateHandlers {
        overrideEffect(lambda)
    }
    return this
}

/**
 * Convenience extension to extend an existing signal handler in a Transformer.
 */
inline fun <reified T : Transmission.Signal> Transformer.extendSignalHandler(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
): Transformer {
    updateHandlers {
        extendSignal(lambda)
    }
    return this
}

/**
 * Convenience extension to override an existing signal handler in a Transformer.
 */
inline fun <reified T : Transmission.Signal> Transformer.overrideSignalHandler(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
): Transformer {
    updateHandlers {
        overrideSignal(lambda)
    }
    return this
}
package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.RequestHandler

/**
 * Throws [IllegalArgumentException] when multiple executions with the same key
 * are defined inside the [Transformer].
 *
 * Adds a execution to [Transformer] to be queried that returns Unit.
 * Can be queried using [RequestHandler.execute]
 * @param execution execution to get the result [Transmission.Data]
 */
fun <C : Contract.Execution> ExecutionScope.register(
    contract: C,
    execution: suspend RequestHandler.() -> Unit,
) {
    this.executionRegistry.buildWith(contract.key, execution)
}

/**
 * Throws [IllegalArgumentException] when multiple executions with the same key
 * are defined inside the [Transformer].
 *
 * Adds a execution to [Transformer] to be queried. This execution accepts any class as Argument
 * that returns Unit.
 * Can be queried using [RequestHandler.execute]
 * @param execution execution to get the result [Transmission.Data]
 */
fun <C : Contract.ExecutionWithArgs<A>, A : Any> ExecutionScope.register(
    contract: C,
    execution: suspend RequestHandler.(args: A) -> Unit,
) {
    this.executionRegistry.buildWith(contract.key, execution)
}

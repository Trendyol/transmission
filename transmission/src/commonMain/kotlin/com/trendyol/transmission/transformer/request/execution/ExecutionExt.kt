package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler

/**
 * Throws [IllegalArgumentException] when multiple executions with the same key
 * are defined inside the [Transformer].
 *
 * Adds a execution to [Transformer] to be queried that returns Unit.
 * Can be queried using [QueryHandler.execute]
 * @param execution execution to get the result [Transmission.Data]
 */
fun ExecutionScope.register(
    contract: Contract.Execution,
    execution: suspend QueryHandler.() -> Unit,
) {
    this.executionRegistry.buildWith(contract.key, execution)
}

/**
 * Throws [IllegalArgumentException] when multiple executions with the same key
 * are defined inside the [Transformer].
 *
 * Adds a execution to [Transformer] to be queried. This execution accepts any class as Argument
 * that returns Unit.
 * Can be queried using [QueryHandler.execute]
 * @param execution execution to get the result [Transmission.Data]
 */
fun <A : Any> ExecutionScope.register(
    contract: Contract.ExecutionWithArgs<A>,
    execution: suspend QueryHandler.(args: A) -> Unit,
) {
    this.executionRegistry.buildWith(contract.key, execution)
}

package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer

class ExecutionScope internal constructor(internal val executionRegistry: ExecutionRegistry)

class Executions internal constructor()

fun Transformer.executions(scope: ExecutionScope.() -> Unit = {}): Executions {
    this.executionRegistry.clear()
    ExecutionScope(executionRegistry).apply(scope)
    return Executions()
}

fun Executions.extendExecutions(
    transformer: Transformer,
    scope: ExecutionScope.() -> Unit = {}
): Executions {
    ExecutionScope(transformer.executionRegistry).apply(scope)
    return Executions()
}

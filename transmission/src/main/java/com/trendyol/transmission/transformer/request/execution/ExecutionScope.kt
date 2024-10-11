package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer

class ExecutionScope internal constructor(internal val executionRegistry: ExecutionRegistry)

class Executions internal constructor()

class ExtendedExecutions internal constructor()

fun Transformer.createExecutions(scope: ExecutionScope.() -> Unit): Executions {
    this.executionRegistry.clear()
    ExecutionScope(executionRegistry).apply(scope)
    return Executions()
}

fun Transformer.extendExecutions(scope: ExecutionScope.() -> Unit): ExtendedExecutions {
    ExecutionScope(executionRegistry).apply(scope)
    return ExtendedExecutions()
}

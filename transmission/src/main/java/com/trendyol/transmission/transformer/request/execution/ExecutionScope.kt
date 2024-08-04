package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer

class ExecutionScope internal constructor(internal val executionRegistry: ExecutionRegistry)

fun Transformer.executionRegistry(scope: ExecutionScope.() -> Unit): ExecutionRegistry {
    val executionRegistry = ExecutionRegistry(this)
    ExecutionScope(executionRegistry).apply(scope)
    return executionRegistry
}

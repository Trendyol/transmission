package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer

class ExecutionScope internal constructor(internal val executionRegistry: ExecutionRegistry)

class Executions internal constructor()

fun Transformer.executions(scope: ExecutionScope.() -> Unit = {}): Executions {
    this.executionRegistry.clear()
    ExecutionScope(executionRegistry).apply(scope)
    return Executions()
}

/**
 * Creates and registers executions for this transformer.
 * 
 * @deprecated Use [executions] instead. This method is kept for binary compatibility.
 * @param scope DSL lambda for defining executions
 * @return An [Executions] instance representing the registered executions
 */
@Deprecated(
    message = "Use executions instead",
    replaceWith = ReplaceWith("executions(scope)"),
    level = DeprecationLevel.WARNING
)
fun Transformer.createExecutions(scope: ExecutionScope.() -> Unit = {}): Executions {
    return executions(scope)
}

fun Executions.extendExecutions(
    transformer: Transformer,
    scope: ExecutionScope.() -> Unit = {}
): Executions {
    ExecutionScope(transformer.executionRegistry).apply(scope)
    return Executions()
}

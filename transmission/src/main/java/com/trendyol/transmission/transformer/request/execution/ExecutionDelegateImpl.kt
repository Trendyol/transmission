package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.request.QueryHandler

internal class ExecutionDelegate(
    private val execution: suspend QueryHandler.() -> Unit,
) : ExecutionOwner.Default {

    override suspend fun execute(scope: QueryHandler) {
        execution.invoke(scope)
    }
}

internal class ExecutionDelegateWithArgs<A : Any>(
    private val execution: suspend QueryHandler.(args: A) -> Unit,
) : ExecutionOwner.WithArgs<A> {

    override suspend fun execute(scope: QueryHandler, args: A) {
        execution(scope, args)
    }
}

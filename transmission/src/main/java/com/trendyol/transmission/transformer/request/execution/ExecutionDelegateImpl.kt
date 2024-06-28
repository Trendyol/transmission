package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.request.RequestHandler

internal class ExecutionDelegate(
    private val execution: suspend RequestHandler.() -> Unit,
) : ExecutionOwner.Default {

    override suspend fun execute(scope: RequestHandler) {
        execution.invoke(scope)
    }
}

internal class ExecutionDelegateWithArgs<A : Any>(
    private val execution: suspend RequestHandler.(args: A) -> Unit,
) : ExecutionOwner.WithArgs<A> {

    override suspend fun execute(scope: RequestHandler, args: A) {
        execution(scope, args)
    }
}

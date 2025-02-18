package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.request.QueryHandler

internal sealed interface ExecutionOwner {

    interface WithArgs<A : Any> : ExecutionOwner {
        suspend fun execute(scope: QueryHandler, args: A)
    }

    interface Default : ExecutionOwner {
        suspend fun execute(scope: QueryHandler)
    }
}

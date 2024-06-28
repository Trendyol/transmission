package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.request.RequestHandler

internal sealed interface ExecutionOwner {

    interface WithArgs<A : Any> : ExecutionOwner {
        suspend fun execute(scope: RequestHandler, args: A)
    }

    interface Default : ExecutionOwner {
        suspend fun execute(scope: RequestHandler)
    }
}

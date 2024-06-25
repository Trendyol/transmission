package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.request.RequestHandler

internal sealed interface ComputationOwner {
    interface WithArgs<A: Any>: ComputationOwner {
        suspend fun getResult(
            scope: RequestHandler,
            invalidate: Boolean = false,
            args: A
        ): Any?
    }

    interface Default: ComputationOwner {
        suspend fun getResult(scope: RequestHandler, invalidate: Boolean = false): Any?
    }

}

package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.request.QueryHandler

internal sealed interface ComputationOwner {
    interface WithArgs<A : Any> : ComputationOwner {
        suspend fun getResult(
            scope: QueryHandler,
            invalidate: Boolean = false,
            args: A
        ): Any?
    }

    interface Default : ComputationOwner {
        suspend fun getResult(scope: QueryHandler, invalidate: Boolean = false): Any?
    }
}

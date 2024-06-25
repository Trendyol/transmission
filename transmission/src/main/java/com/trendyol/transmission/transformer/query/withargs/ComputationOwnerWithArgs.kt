package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.query.RequestHandler

internal interface ComputationOwnerWithArgs<A : Any> {
    suspend fun getResult(
        scope: RequestHandler,
        invalidate: Boolean = false,
        args: A
    ): Any?
}

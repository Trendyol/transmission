package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.query.QuerySender

internal interface ComputationOwnerWithArgs<A : Any> {
    suspend fun getResult(
        scope: QuerySender,
        invalidate: Boolean = false,
        args: A
    ): Transmission.Data?
}

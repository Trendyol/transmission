package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.query.QuerySender

internal class ComputationDelegateWithArgs<A : Any>(
    private val useCache: Boolean = false,
    private val computation: suspend QuerySender.(args: A) -> Transmission.Data?
) : ComputationOwnerWithArgs<A> {

    private var result: Transmission.Data? = null

    override suspend fun getResult(
        scope: QuerySender,
        invalidate: Boolean,
        args: A
    ): Transmission.Data? {
        return if (useCache && invalidate.not()) {
            result ?: computation(scope, args).also { result = it }
        } else {
            result = null
            computation(scope, args)
        }
    }
}

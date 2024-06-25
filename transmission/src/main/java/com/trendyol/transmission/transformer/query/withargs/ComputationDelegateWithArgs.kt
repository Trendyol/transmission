package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.transformer.query.RequestHandler

internal class ComputationDelegateWithArgs<A : Any>(
    private val useCache: Boolean = false,
    private val computation: suspend RequestHandler.(args: A) -> Any?
) : ComputationOwnerWithArgs<A> {

    private var result: Any? = null

    override suspend fun getResult(
        scope: RequestHandler,
        invalidate: Boolean,
        args: A
    ): Any? {
        return if (useCache && invalidate.not()) {
            result ?: computation(scope, args).also { result = it }
        } else {
            result = null
            computation(scope, args)
        }
    }
}

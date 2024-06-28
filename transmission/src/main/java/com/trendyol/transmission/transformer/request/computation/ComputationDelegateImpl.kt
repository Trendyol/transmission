package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.request.RequestHandler

internal class ComputationDelegate(
    private val useCache: Boolean = false,
    private val computation: (suspend RequestHandler.() -> Any?)? = null,
) : ComputationOwner.Default {

    private var result: Any? = null

    override suspend fun getResult(scope: RequestHandler, invalidate: Boolean): Any? {
        return if (useCache && invalidate.not()) {
            result ?: computation?.invoke(scope).also { result = it }
        } else {
            result = null
            computation?.invoke(scope)
        }
    }
}

internal class ComputationDelegateWithArgs<A : Any>(
    private val useCache: Boolean = false,
    private val computation: suspend RequestHandler.(args: A) -> Any?,
) : ComputationOwner.WithArgs<A> {

    private var result: Any? = null

    override suspend fun getResult(scope: RequestHandler, invalidate: Boolean, args: A): Any? {
        return if (useCache && invalidate.not()) {
            result ?: computation(scope, args).also { result = it }
        } else {
            result = null
            computation(scope, args)
        }
    }
}

package com.trendyol.transmission.transformer.query

internal class ComputationDelegate(
    private val useCache: Boolean = false,
    private val computation: suspend RequestHandler.() -> Any?
) : ComputationOwner {

    private var result: Any? = null

    override suspend fun getResult(scope: RequestHandler, invalidate: Boolean): Any? {
        return if (useCache && invalidate.not()) {
            result ?: computation(scope).also { result = it }
        } else {
            result = null
            computation(scope)
        }
    }
}

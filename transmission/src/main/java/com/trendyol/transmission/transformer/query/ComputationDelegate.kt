package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

class ComputationDelegate<D : Transmission.Data, E : Transmission.Effect>(
    private val useCache: Boolean = false,
    private val computation: suspend QuerySender<D, E>.() -> D?
) : ComputationOwner<D, E> {

    private var result: D? = null

    override suspend fun getResult(scope: QuerySender<D, E>, invalidate: Boolean): D? {
        return if (useCache && invalidate.not()) {
            result ?: computation(scope).also { result = it }
        } else {
            result = null
            computation(scope)
        }
    }
}

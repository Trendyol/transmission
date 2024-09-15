package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission

sealed class QueryResult(
    open val owner: String,
    open val key: String,
) {
    class Data<D : Transmission.Data>(
        override val owner: String,
        override val key: String,
        val data: D?,
    ) : QueryResult(owner, key)

    class Computation<D : Any>(
        override val owner: String,
        override val key: String,
        val data: D?,
    ) : QueryResult(owner, key)
}

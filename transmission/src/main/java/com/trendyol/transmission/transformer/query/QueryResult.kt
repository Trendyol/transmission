package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

sealed class QueryResult<D : Transmission.Data>(
    open val owner: String,
    open val key: String,
    open val data: D?,
) {
    data class Data<D : Transmission.Data>(
        override val owner: String,
        override val key: String,
        override val data: D?,
    ) : QueryResult<D>(owner, key, data)

    data class Computation<D : Transmission.Data>(
        override val owner: String,
        override val key: String,
        override val data: D?,
    ) : QueryResult<D>(owner, key, data)
}

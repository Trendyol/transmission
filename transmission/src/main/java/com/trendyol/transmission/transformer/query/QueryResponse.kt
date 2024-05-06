package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

sealed class QueryResponse<D : Transmission.Data>(
    open val owner: String,
    open val data: D?,
    open val type: String
) {
    data class Data<D : Transmission.Data>(
        override val owner: String,
        override val data: D?,
        override val type: String
    ) :
        QueryResponse<D>(owner, data, type)

    data class Computation<D : Transmission.Data>(
        override val owner: String,
        override val data: D?,
        override val type: String,
    ) : QueryResponse<D>(owner = owner, data = data, type = type)
}

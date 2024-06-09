package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

interface QuerySender {

    suspend fun <D : Transmission.Data> queryData(key: String): D?

    suspend fun <D : Transmission.Data> queryComputation(
        key: String,
        invalidate: Boolean = false,
    ): D?
}

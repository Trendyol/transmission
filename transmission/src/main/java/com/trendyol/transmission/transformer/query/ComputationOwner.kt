package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

internal interface ComputationOwner {
    suspend fun getResult(scope: QuerySender, invalidate: Boolean = false): Transmission.Data?
}

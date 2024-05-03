package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

interface ComputationOwner<D : Transmission.Data, E : Transmission.Effect> {
    suspend fun getResult(scope: QuerySender<D, E>, invalidate: Boolean = false): D?
}

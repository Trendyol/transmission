package com.trendyol.transmission.transformer.query

internal interface ComputationOwner {
    suspend fun getResult(scope: RequestHandler, invalidate: Boolean = false): Any?
}

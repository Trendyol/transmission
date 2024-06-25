package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class ComputationBuilder<T : Transmission.Data> {
    fun buildWith(
        key: String,
        useCache: Boolean = false,
        transformer: Transformer,
        computation: suspend RequestHandler.() -> T?
    ) {
        transformer.storage.registerComputation(
            key = key,
            delegate = ComputationDelegate(useCache = useCache, computation = computation)
        )
    }
}

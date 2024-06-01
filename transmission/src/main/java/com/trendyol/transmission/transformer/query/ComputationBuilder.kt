package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class ComputationBuilder<T : Transmission.Data> {
    fun buildWith(
        typeName: String,
        useCache: Boolean = false,
        transformer: Transformer,
        computation: suspend QuerySender.() -> T?
    ) {
        transformer.storage.registerComputation(
            name = typeName,
            delegate = ComputationDelegate(useCache = useCache, computation = computation)
        )
    }
}

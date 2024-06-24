package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender

class ComputationBuilderWithArgs<A : Any, T : Transmission.Data> {
    fun buildWith(
        key: String,
        useCache: Boolean = false,
        transformer: Transformer,
        computation: suspend QuerySender.(args: A) -> T?
    ) {
        transformer.storage.registerComputationWithArgs(
            key = key,
            delegate = ComputationDelegateWithArgs(useCache = useCache, computation = computation)
        )
    }
}

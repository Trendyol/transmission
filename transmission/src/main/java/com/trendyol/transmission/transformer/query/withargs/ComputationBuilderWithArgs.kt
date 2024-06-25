package com.trendyol.transmission.transformer.query.withargs

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.RequestHandler

class ComputationBuilderWithArgs<A : Any, T : Any> {
    fun buildWith(
        key: String,
        useCache: Boolean = false,
        transformer: Transformer,
        computation: suspend RequestHandler.(args: A) -> T?
    ) {
        transformer.storage.registerComputationWithArgs(
            key = key,
            delegate = ComputationDelegateWithArgs(useCache = useCache, computation = computation)
        )
    }
}

package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.RequestHandler

class ComputationBuilder {

    fun <T : Any> buildWith(
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

    fun <A : Any, T : Any> buildWith(
        key: String,
        useCache: Boolean = false,
        transformer: Transformer,
        computation: suspend RequestHandler.(args: A) -> T?
    ) {
        transformer.storage.registerComputation(
            key = key,
            delegate = ComputationDelegateWithArgs(useCache = useCache, computation = computation)
        )
    }
}

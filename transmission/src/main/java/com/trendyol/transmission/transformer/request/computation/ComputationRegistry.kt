package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.RequestHandler

class ComputationRegistry internal constructor(private val transformer: Transformer) {

    internal fun <T : Any> buildWith(
        key: String,
        useCache: Boolean = false,
        computation: suspend RequestHandler.() -> T?
    ) {
        transformer.storage.registerComputation(
            key = key,
            delegate = ComputationDelegate(useCache = useCache, computation = computation)
        )
    }

    internal fun <A : Any, T : Any> buildWith(
        key: String,
        useCache: Boolean = false,
        computation: suspend RequestHandler.(args: A) -> T?
    ) {
        transformer.storage.registerComputation(
            key = key,
            delegate = ComputationDelegateWithArgs(useCache = useCache, computation = computation)
        )
    }
}

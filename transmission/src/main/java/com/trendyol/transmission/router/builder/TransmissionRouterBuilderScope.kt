package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.Default

interface TransmissionRouterBuilderScope {
    /**
     * Sets the [CoroutineDispatcher] to the [TransmissionRouter].
     * If not provided, [Default] Dispatcher is going to be used.
     */
    fun withDispatcher(dispatcher: CoroutineDispatcher)

    /**
     * Sets the [Transformer] set to the [TransmissionRouter].
     * Either this method or [withLoader] must be used to provide a valid
     * set of [Transformer]s.
     */
    fun withTransformerSet(transformerSet: Set<Transformer>)

    /**
     * Sets [TransformerSetLoader] to the [TransmissionRouter].
     * Either this method or [withTransformerSet] must be used to provide a valid
     * set of [Transformer]s.
     */
    fun withLoader(loader: TransformerSetLoader)
}

package com.trendyol.transmission.router.builder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.Default

interface TransmissionRouterBuilderScope {
    /**
     * Sets the [CoroutineDispatcher] to the [TransmissionRouter].
     * If not provided, [Default] Dispatcher is going to be used.
     */
    fun addDispatcher(dispatcher: CoroutineDispatcher)

    /**
     * Sets the [Transformer] set to the [TransmissionRouter].
     * In cases where [overrideInitialization] is not used,
     * Either this method or [addLoader] must be used to provide a valid
     * set of [Transformer]s.
     */
    fun addTransformerSet(transformerSet: Set<Transformer>)

    /**
     * Sets [Capacity] to the [TransmissionRouter]. This affects the buffer capacity of
     * [Transmission.Data], [Transmission.Signal] and [Transmission.Effect] streams inside the
     * Router. The higher the value, better the [TransmissionRouter] can handle more [Transformer]
     * communication without suspension.
     */
    fun setCapacity(capacity: Capacity)

    /**
     * Sets [TransformerSetLoader] to the [TransmissionRouter].
     * In cases where [overrideInitialization] is not used, Either this method or
     * [addTransformerSet] must be used to provide a valid
     * set of [Transformer]s.
     */
    fun addLoader(loader: TransformerSetLoader)

    /**
     * Overrides auto initialization of [TransmissionRouter]. This disables any effect of using
     * [addLoader] or [addTransformerSet].
     */
    fun overrideInitialization()
}

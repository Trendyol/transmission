package com.trendyol.transmission.router.builder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.Default

/**
 * DSL scope for configuring [TransmissionRouter] during creation.
 * 
 * This interface provides methods to configure various aspects of a TransmissionRouter
 * including transformers, dispatchers, capacity, and initialization behavior.
 * 
 * @see TransmissionRouter for router creation
 */
interface TransmissionRouterBuilderScope {
    /**
     * Sets the [CoroutineDispatcher] for the [TransmissionRouter].
     * 
     * The dispatcher controls which thread pool the router uses for processing
     * signals, effects, and data. If not specified, [Dispatchers.Default] is used.
     * 
     * @param dispatcher The coroutine dispatcher to use for router operations
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     addDispatcher(Dispatchers.IO) // Use IO dispatcher for network-heavy operations
     * }
     * ```
     */
    fun addDispatcher(dispatcher: CoroutineDispatcher)

    /**
     * Sets the [Transformer] set for the [TransmissionRouter].
     * 
     * Provides the collection of transformers that will process signals and effects.
     * Either this method or [addLoader] must be used when auto-initialization is enabled.
     * 
     * @param transformerSet Set of transformers to register with the router
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     addTransformerSet(setOf(
     *         UserTransformer(),
     *         DataTransformer(),
     *         NetworkTransformer()
     *     ))
     * }
     * ```
     * 
     * @see addLoader for dynamic transformer loading
     * @see overrideInitialization for manual initialization
     */
    fun addTransformerSet(transformerSet: Set<Transformer>)

    /**
     * Sets the buffer [Capacity] for the [TransmissionRouter].
     * 
     * Controls the buffer size of internal channels for signals, effects, and data.
     * Higher capacity allows better handling of concurrent processing but uses more memory.
     * 
     * @param capacity The buffer capacity configuration
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     setCapacity(Capacity.Custom(256)) // Higher capacity for intensive processing
     * }
     * ```
     * 
     * @see Capacity for available capacity options
     */
    fun setCapacity(capacity: Capacity)

    /**
     * Sets a [TransformerSetLoader] for dynamic transformer loading.
     * 
     * Provides a loader that can dynamically provide transformers to the router.
     * Either this method or [addTransformerSet] must be used when auto-initialization is enabled.
     * 
     * @param loader The transformer set loader
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     addLoader(MyTransformerSetLoader())
     * }
     * ```
     * 
     * @see addTransformerSet for direct transformer provision
     * @see overrideInitialization for manual initialization
     */
    fun addLoader(loader: TransformerSetLoader)

    /**
     * Disables automatic initialization of the [TransmissionRouter].
     * 
     * When called, the router will not automatically initialize transformers during creation.
     * Manual initialization must be performed later using [TransmissionRouter.initialize].
     * This disables the effect of [addLoader] and [addTransformerSet].
     * 
     * Example usage:
     * ```kotlin
     * val router = TransmissionRouter {
     *     overrideInitialization()
     * }
     * 
     * // Later, manually initialize
     * router.initialize(MyTransformerSetLoader())
     * ```
     * 
     * @see TransmissionRouter.initialize for manual initialization
     */
    fun overrideInitialization()
}

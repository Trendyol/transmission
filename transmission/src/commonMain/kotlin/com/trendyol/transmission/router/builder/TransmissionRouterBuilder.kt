package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.request.Contract

/**
 * Creates a new [TransmissionRouter] for managing signal processing and data flow in reactive applications.
 * 
 * The router acts as a central hub that coordinates communication between transformers,
 * handling signal routing, effect processing, and data streaming. It provides a structured
 * way to process user signals through a chain of transformers that can produce effects and data.
 * 
 * @param identity Unique identifier for this router instance. If not provided, a random identifier is generated.
 * @param scope Configuration lambda for setting up the router with transformers, capacity, and dispatcher
 * @return A configured [TransmissionRouter] instance ready to process signals
 * 
 * @throws IllegalStateException if no transformers are provided and auto-initialization is enabled
 * 
 * Example usage:
 * ```kotlin
 * val router = TransmissionRouter {
 *     addTransformerSet(setOf(myTransformer1, myTransformer2))
 *     setCapacity(Capacity.Custom(256))
 *     addDispatcher(Dispatchers.Default)
 * }
 * 
 * // Process signals
 * router.process(MySignal.UserAction("data"))
 * 
 * // Observe data
 * router.streamData<MyData>().collect { data ->
 *     // Handle data updates
 * }
 * ```
 * 
 * @see TransmissionRouterBuilderScope for available configuration options
 * @see com.trendyol.transmission.transformer.Transformer for creating transformers
 * @see com.trendyol.transmission.router.streamData for observing data streams
 */
fun TransmissionRouter(
    identity: Contract.Identity = Contract.identity(),
    scope: TransmissionRouterBuilderScope.() -> Unit
): TransmissionRouter {
    val builder = TransmissionRouterBuilderScopeImpl(scope)
    return TransmissionRouter(
        identity = identity,
        transformerSetLoader =
            builder.transformerSetLoader.takeIf { builder.autoInitialization },
        autoInitialization = builder.autoInitialization,
        capacity = builder.capacity,
        dispatcher = builder.dispatcher,
    )
}

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.checkpoint.CheckpointHandler
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler

/**
 * Provides communication capabilities for transformers to interact with the router and other transformers.
 * 
 * CommunicationScope is the primary interface available within transformer handlers for:
 * - Sending data back to the router for emission to observers
 * - Publishing effects to other transformers
 * - Performing inter-transformer queries and computations
 * - Managing checkpoint-based flow control
 * 
 * This interface extends [QueryHandler] and [CheckpointHandler] to provide a comprehensive
 * communication API within transformer processing contexts.
 * 
 * @see QueryHandler for inter-transformer query capabilities
 * @see CheckpointHandler for checkpoint-based flow control
 * @see com.trendyol.transmission.transformer.handler.handlers for creating handlers
 */
interface CommunicationScope : QueryHandler, CheckpointHandler {
    /**
     * Sends data to the [TransmissionRouter] for emission to data stream observers.
     * 
     * This is the primary method for transformers to emit processed data that represents
     * application state. The data will be broadcast to all data stream observers.
     * 
     * @param D The type of data to send, must extend [Transmission.Data]
     * @param data The data instance to send, can be null
     * 
     * Example usage:
     * ```kotlin
     * onSignal<UserSignal.Login> { signal ->
     *     val user = authenticateUser(signal.credentials)
     *     send(UserData.LoggedIn(user)) // Emits to data stream
     * }
     * ```
     * 
     * @see TransmissionRouter.streamData for observing sent data
     */
    suspend fun <D : Transmission.Data> send(data: D?)

    /**
     * Publishes an effect to all transformers for potential processing.
     * 
     * Effects represent side effects or inter-transformer communications. This method
     * broadcasts the effect to all transformers that have registered handlers for the effect type.
     * 
     * @param E The type of effect to publish, must extend [Transmission.Effect]
     * @param effect The effect instance to publish
     * 
     * Example usage:
     * ```kotlin
     * onSignal<UserSignal.Logout> {
     *     publish(CacheEffect.ClearUserCache) // Notify other transformers
     *     send(UserData.LoggedOut)
     * }
     * ```
     * 
     * @see com.trendyol.transmission.transformer.handler.onEffect for handling effects
     */
    suspend fun <E : Transmission.Effect> publish(effect: E)

    /**
     * Sends arbitrary payload data through the router as a one-shot effect.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method allows sending typed payload data that can be observed using the
     * `oneShotPayloadStream` function. Useful for sending structured data that doesn't
     * fit the standard Signal/Effect/Data pattern.
     * 
     * @param D The type of payload data to send
     * @param payload The payload data to send
     * 
     * Example usage:
     * ```kotlin
     * onSignal<ErrorSignal> { signal ->
     *     sendPayload(ErrorInfo(signal.message, signal.code))
     * }
     * ```
     * 
     * @see com.trendyol.transmission.router.oneShotPayloadStream for observing payloads
     */
    @ExperimentalTransmissionApi
    suspend fun <D: Any> sendPayload(payload: D)

    /**
     * Sends an effect to a specific transformer identified by its contract identity.
     * 
     * This method provides targeted communication between transformers, allowing one
     * transformer to send effects directly to another specific transformer rather than
     * broadcasting to all transformers.
     * 
     * @param E The type of effect to send, must extend [Transmission.Effect]
     * @param effect The effect instance to send
     * @param identity The target transformer's identity contract
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val cacheTransformerIdentity = Contract.identity()
     * }
     * 
     * onSignal<DataSignal.Refresh> {
     *     send(CacheEffect.Invalidate, cacheTransformerIdentity)
     * }
     * ```
     * 
     * @see Contract.Identity for creating transformer identities
     */
    suspend fun <E : Transmission.Effect> send(effect: E, identity: Contract.Identity)
}

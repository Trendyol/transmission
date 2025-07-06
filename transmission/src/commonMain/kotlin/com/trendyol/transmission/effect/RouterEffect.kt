package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer

/**
 * Special effect type for sending payload directly to the router without inter-transformer broadcasting.
 * 
 * RouterEffect is a unique effect type that bypasses the normal effect broadcasting mechanism.
 * Instead of being distributed to all transformers like regular effects, RouterEffect instances
 * are handled directly by the router, making them ideal for router-specific operations and
 * direct payload transmission.
 * 
 * Key characteristics:
 * - Not published to other transformers
 * - Processed directly by the router
 * - Suitable for router control operations
 * - Can carry arbitrary payload data
 * 
 * @param payload Arbitrary data to send directly to the router
 * 
 * Example usage:
 * ```kotlin
 * // In a transformer
 * onSignal<ConfigSignal.UpdateCapacity> { signal ->
 *     // Send configuration directly to router without notifying other transformers
 *     publish(RouterEffect(CapacityConfig(signal.newCapacity)))
 * }
 * ```
 * 
 * @see Transmission.Effect for the base effect interface
 * @see CommunicationScope.publish for publishing effects
 */
data class RouterEffect(val payload: Any): Transmission.Effect

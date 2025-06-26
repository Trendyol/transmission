package com.trendyol.transmission.router

import kotlinx.coroutines.channels.Channel
import kotlin.jvm.JvmInline

/**
 * Defines the buffer capacity for internal channels in [TransmissionRouter].
 * 
 * Capacity affects the buffer size of signal, effect, and data channels within the router.
 * Higher capacity values allow the router to handle more concurrent processing without
 * suspending senders, but consume more memory.
 * 
 * @see TransmissionRouter
 * @see TransmissionRouterBuilderScope.setCapacity
 */
@JvmInline
value class Capacity private constructor(val value: Int) {
    companion object {
        /**
         * Default capacity of 64 elements, suitable for most applications.
         */
        val Default = Capacity(64)

        /**
         * Custom capacity with a specified buffer size.
         * 
         * @param capacity The number of elements that can be buffered. Must be positive.
         * 
         * Example usage:
         * ```kotlin
         * val router = TransmissionRouter {
         *     setCapacity(Capacity.Custom(256)) // Higher capacity for intensive processing
         * }
         * ```
         */
        fun Custom(value: Int): Capacity {
            require(value in 0..256) { "bufferCapacity should be between 0 and 256" }
            return Capacity(value)
        }

        val Unlimited = Capacity(Channel.UNLIMITED)
    }
}

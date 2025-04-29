package com.trendyol.transmission.router

import kotlinx.coroutines.channels.Channel
import kotlin.jvm.JvmInline

@JvmInline
value class Capacity private constructor(val value: Int) {
    companion object {
        val Default = Capacity(64)

        fun Custom(value: Int): Capacity {
            require(value in 0..256) { "bufferCapacity should be between 0 and 256" }
            return Capacity(value)
        }

        val Unlimited = Capacity(Channel.UNLIMITED)
    }
}

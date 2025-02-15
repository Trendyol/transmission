package com.trendyol.transmission.router

sealed class Capacity(val value: Int) {

    init {
        check(value in 0..256) {
            "bufferCapacity should be between 0 and 256"
        }
    }

    data object Default : Capacity(64)

    data class Custom(val customValue: Int) : Capacity(customValue)
}
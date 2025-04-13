package com.trendyol.transmission.counter

import com.trendyol.transmission.Transmission

sealed interface CounterSignal : Transmission.Signal {
    data object Lookup : CounterSignal
}

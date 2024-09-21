package com.trendyol.transmission.components.features.input

import com.trendyol.transmission.Transmission

sealed interface InputSignal : Transmission.Signal {
    data class InputUpdate(val value: String) : InputSignal
}

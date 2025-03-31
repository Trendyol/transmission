package com.trendyol.transmission.components.input

import com.trendyol.transmission.Transmission

sealed interface InputEffect : Transmission.Effect {
    data class InputUpdate(val value: String) : InputEffect
}

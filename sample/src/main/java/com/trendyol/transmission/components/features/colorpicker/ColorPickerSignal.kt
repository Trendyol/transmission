package com.trendyol.transmission.components.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.Transmission

sealed interface ColorPickerSignal : Transmission.Signal {
    data class SelectColor(val index: Int, val selectedColor: Color) : ColorPickerSignal
}

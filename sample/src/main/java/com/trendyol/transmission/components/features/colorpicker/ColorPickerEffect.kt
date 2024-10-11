package com.trendyol.transmission.components.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.Transmission

sealed interface ColorPickerEffect : Transmission.Effect {
    data class BackgroundColorUpdate(val color: Color) : ColorPickerEffect
    data class SelectedColorUpdate(val color: Color) : ColorPickerEffect
}

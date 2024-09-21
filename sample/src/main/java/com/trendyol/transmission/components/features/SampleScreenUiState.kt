package com.trendyol.transmission.components.features

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.Transmission

data class InputUiState(
    val writtenText: String = "",
    val backgroundColor: Color = Color.White
) : Transmission.Data

data class OutputUiState(
    val outputText: String = "",
    val backgroundColor: Color = Color.White
) : Transmission.Data

data class ColorPickerUiState(
    val selectedColorIndex: Int = 0,
    val backgroundColor: Color = Color.White
) : Transmission.Data

data class MultiOutputUiState(
    val writtenUppercaseText: String = "",
    val backgroundColor: Color = Color.White,
    val selectedColor: Color = Color.White
) : Transmission.Data

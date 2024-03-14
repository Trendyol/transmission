package com.trendyol.transmission.ui

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.Transmission


data class SampleScreenUiState(
	val inputState: InputUiState = InputUiState(),
	val outputState: OutputUiState = OutputUiState(),
	val colorPickerState: ColorPickerUiState = ColorPickerUiState(),
	val multiOutputState: MultiOutputUiState = MultiOutputUiState()
)

data class InputUiState(
	val writtenText: String = "",
	val backgroundColor: Color = Color.White
): Transmission.Data

data class OutputUiState(
	val outputText: String = "",
	val backgroundColor: Color = Color.White
): Transmission.Data

data class ColorPickerUiState(
	val selectedColorIndex: Int = 0,
	val backgroundColor: Color = Color.White
): Transmission.Data

data class MultiOutputUiState(
	val writtenUppercaseText: String = "",
	val backgroundColor: Color = Color.White,
	val selectedColor: Color = Color.White
): Transmission.Data

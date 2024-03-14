package com.trendyol.transmission.features.input

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.ui.InputUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class InputTransformer @Inject constructor() : Transformer() {

	private val _inputState = MutableStateFlow(InputUiState())
	private val inputState = _inputState.reflectUpdates()

	override suspend fun onSignal(signal: Transmission.Signal) {
		when (signal) {
			is InputSignal.InputUpdate -> {
				_inputState.update { it.copy(writtenText = signal.value) }
				sendEffect(InputEffect.InputUpdate(signal.value))
			}
		}
	}

	override suspend fun onEffect(effect: Transmission.Effect) {
		when (effect) {
			is ColorPickerEffect.BackgroundColorUpdate -> {
				_inputState.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}

}

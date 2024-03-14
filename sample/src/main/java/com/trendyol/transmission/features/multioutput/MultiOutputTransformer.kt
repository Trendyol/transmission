package com.trendyol.transmission.features.multioutput

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.ui.MultiOutputUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class MultiOutputTransformer @Inject constructor() : Transformer() {

	private val _multiOutput = MutableStateFlow(MultiOutputUiState())
	private val multiOutput = _multiOutput.reflectUpdates()


	override suspend fun onSignal(signal: Transmission.Signal) {
	}

	override suspend fun onEffect(effect: Transmission.Effect) {
		when (effect) {
			is InputEffect.InputUpdate -> {
				_multiOutput.update { it.copy(writtenUppercaseText = effect.value.uppercase()) }
			}
			is ColorPickerEffect.BackgroundColorUpdate -> {
				_multiOutput.update { it.copy(backgroundColor = effect.color) }
			}
			is ColorPickerEffect.SelectedColorUpdate-> {
				_multiOutput.update { it.copy(selectedColor = effect.color) }
			}
		}
	}

}
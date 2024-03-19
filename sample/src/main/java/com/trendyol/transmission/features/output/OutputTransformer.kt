package com.trendyol.transmission.features.output

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.ui.OutputUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class OutputTransformer @Inject constructor() : Transformer() {

	private val _outputState = MutableStateFlow(OutputUiState()).reflectUpdates()

	override val effectHandler: EffectHandler = EffectHandler { effect ->
		when (effect) {
			is InputEffect.InputUpdate -> {
				_outputState.update { it.copy(outputText = effect.value) }
			}

			is ColorPickerEffect.BackgroundColorUpdate -> {
				_outputState.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}
}

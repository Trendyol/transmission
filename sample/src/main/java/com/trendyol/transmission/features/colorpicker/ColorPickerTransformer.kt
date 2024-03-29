package com.trendyol.transmission.features.colorpicker

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericSignalHandler
import com.trendyol.transmission.ui.ColorPickerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ColorPickerTransformer @Inject constructor() : Transformer() {

	private val _colorPickerState = MutableStateFlow(ColorPickerUiState()).reflectUpdates()

	override val signalHandler: SignalHandler = buildGenericSignalHandler { signal ->
		when (signal) {
			is ColorPickerSignal.SelectColor -> {
				_colorPickerState.update { it.copy(selectedColorIndex = signal.index) }
				publishEffect(
					ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
				)
				publishEffect(ColorPickerEffect.SelectedColorUpdate(signal.selectedColor))
			}
		}
	}

	override val effectHandler: EffectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is ColorPickerEffect.BackgroundColorUpdate -> {
				_colorPickerState.update {
					it.copy(backgroundColor = effect.color)
				}
			}
		}
	}
}

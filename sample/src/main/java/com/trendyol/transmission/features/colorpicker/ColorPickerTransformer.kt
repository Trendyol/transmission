package com.trendyol.transmission.features.colorpicker

import com.trendyol.transmission.transformer.DefaultTransformer
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericSignalHandler
import com.trendyol.transmission.ui.ColorPickerUiState
import javax.inject.Inject

class ColorPickerTransformer @Inject constructor() : DefaultTransformer() {

	private val holder = TransmissionDataHolder(ColorPickerUiState())

	override val signalHandler = buildGenericSignalHandler { signal ->
		when (signal) {
			is ColorPickerSignal.SelectColor -> {
				holder.update { it.copy(selectedColorIndex = signal.index) }
				publishEffect(
					ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
				)
				publishEffect(ColorPickerEffect.SelectedColorUpdate(signal.selectedColor))
			}
		}
	}


	override val effectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is ColorPickerEffect.BackgroundColorUpdate -> {
				holder.update {
					it.copy(backgroundColor = effect.color)
				}
			}
		}
	}
}

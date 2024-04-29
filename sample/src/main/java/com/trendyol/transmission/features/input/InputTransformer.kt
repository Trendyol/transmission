package com.trendyol.transmission.features.input

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.DefaultTransformer
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.InputUiState
import javax.inject.Inject

class InputTransformer @Inject constructor() : DefaultTransformer() {

	private val holder = buildDataHolder(InputUiState())

	override val signalHandler = buildTypedSignalHandler<InputSignal> { signal ->
		when (signal) {
			is InputSignal.InputUpdate -> {
				holder.update { it.copy(writtenText = signal.value) }
				publishEffect(InputEffect.InputUpdate(signal.value))
			}
		}
	}

	override val effectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is ColorPickerEffect.BackgroundColorUpdate -> {
				holder.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}
}

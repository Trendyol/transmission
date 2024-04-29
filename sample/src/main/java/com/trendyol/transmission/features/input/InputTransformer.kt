package com.trendyol.transmission.features.input

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildTypedSignalHandler
import com.trendyol.transmission.ui.InputUiState
import javax.inject.Inject

class InputTransformer @Inject constructor() : Transformer<Transmission.Data>() {

	private val holder = TransmissionDataHolder(InputUiState())

	override val signalHandler = buildTypedSignalHandler<Transmission.Data, InputSignal> { signal ->
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

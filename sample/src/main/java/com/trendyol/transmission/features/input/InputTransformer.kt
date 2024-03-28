package com.trendyol.transmission.features.input

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildTypedSignalHandler
import com.trendyol.transmission.ui.InputUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class InputTransformer @Inject constructor() : Transformer() {

	private val _inputState = MutableStateFlow(InputUiState()).reflectUpdates()

	override val signalHandler: SignalHandler = buildTypedSignalHandler<InputSignal> { signal ->
		when (signal) {
			is InputSignal.InputUpdate -> {
				_inputState.update { it.copy(writtenText = signal.value) }
				publishEffect(InputEffect.InputUpdate(signal.value))
			}
		}
	}

	override val effectHandler: EffectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is ColorPickerEffect.BackgroundColorUpdate -> {
				_inputState.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}
}

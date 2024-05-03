package com.trendyol.transmission.features.input

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.DefaultTransformer
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.InputUiState
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class InputTransformer @Inject constructor() : DefaultTransformer() {

	private val holder = buildDataHolder(InputUiState())

	init {
		registerComputation<WrittenInput> {
			delay(1.seconds)
			WrittenInput(holder.value.writtenText)
		}
	}

	override val signalHandler = buildTypedSignalHandler<InputSignal> { signal ->
		when (signal) {
			is InputSignal.InputUpdate -> {
				holder.update { it.copy(writtenText = signal.value) }
				publish(effect = InputEffect.InputUpdate(signal.value))
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

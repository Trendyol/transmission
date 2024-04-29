package com.trendyol.transmission.features.multioutput

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.MultiOutputUiState
import javax.inject.Inject

class MultiOutputTransformer @Inject constructor() : Transformer() {

	private val holder = TransmissionDataHolder(MultiOutputUiState())

	override val effectHandler: EffectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is InputEffect.InputUpdate -> {
				holder.update { it.copy(writtenUppercaseText = effect.value.uppercase()) }
			}

			is ColorPickerEffect.BackgroundColorUpdate -> {
				holder.update { it.copy(backgroundColor = effect.color) }
			}

			is ColorPickerEffect.SelectedColorUpdate -> {
				holder.update { it.copy(selectedColor = effect.color) }
			}
		}
	}
}

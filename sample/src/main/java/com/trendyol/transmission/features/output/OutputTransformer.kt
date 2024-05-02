package com.trendyol.transmission.features.output

import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.DefaultTransformer
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.InputUiState
import com.trendyol.transmission.ui.OutputUiState
import kotlinx.coroutines.delay
import javax.inject.Inject

class OutputTransformer @Inject constructor() : DefaultTransformer() {

	private val holder = buildDataHolder(OutputUiState())

	override val effectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is InputEffect.InputUpdate -> {
				delay(2000L)
				val output = queryData(InputUiState::class)
				output?.let { testData ->
					holder.update { it.copy(outputText = testData.writtenText) }
				}
				delay(2000L)
				val selectedColor = queryData(ColorPickerUiState::class)
				holder.update {
					it.copy(outputText = it.outputText + " and Selected color index is ${selectedColor?.selectedColorIndex}")
				}
				publishEffect(RouterPayloadEffect(holder.value))
			}

			is ColorPickerEffect.BackgroundColorUpdate -> {
				holder.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}
}

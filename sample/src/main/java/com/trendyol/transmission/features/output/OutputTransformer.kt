package com.trendyol.transmission.features.output

import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.features.input.WrittenInput
import com.trendyol.transmission.transformer.DefaultTransformer
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class OutputTransformer @Inject constructor() : DefaultTransformer() {

	private val holder = buildDataHolder(OutputUiState())

	private val holder2 = buildDataHolder(ColorPickerUiState())

	init {
		registerComputation<OutputCalculationResult> {
			delay(2.seconds)
			val data = queryData(ColorPickerUiState::class)
			val writtenOutput = data?.selectedColorIndex.toString()
			val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
			OutputCalculationResult("result is $result with ($writtenOutput)")
		}
	}

	override val effectHandler = buildGenericEffectHandler { effect ->
		when (effect) {
			is InputEffect.InputUpdate -> {
				holder.update { it.copy(outputText = effect.value) }
				delay(3.seconds)
				val selectedColor =
					queryData(ColorPickerUiState::class, owner = ColorPickerTransformer::class)
				holder.update {
					it.copy(outputText = it.outputText + " and Selected color index is ${selectedColor?.selectedColorIndex}")
				}
				publishEffect(RouterPayloadEffect(holder.value))
				delay(1.seconds)
				sendEffect(
					ColorPickerEffect.BackgroundColorUpdate(holder2.value.backgroundColor),
					to = ColorPickerTransformer::class
				)
			}

			is ColorPickerEffect.BackgroundColorUpdate -> {
				holder.update { it.copy(backgroundColor = effect.color) }
			}
		}
	}
}

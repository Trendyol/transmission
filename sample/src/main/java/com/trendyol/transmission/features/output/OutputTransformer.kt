package com.trendyol.transmission.features.output

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.input.WrittenInput
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.query.registerComputation
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class OutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher) {

    private val holder = buildDataHolder(OutputUiState())

    private val holder2 = buildDataHolder(ColorPickerUiState(), publishUpdates = false)

    init {
        registerComputation<OutputCalculationResult>(key = "OutputCalculation") {
            delay(2.seconds)
            val data = queryData<ColorPickerUiState>("ColorPickerUiState")?.selectedColorIndex
            val writtenOutput = queryComputation<WrittenInput>(key = "WrittenInput")
            val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
            OutputCalculationResult("result is $result with ($writtenOutput) and $data")
        }
    }

    override val effectHandler = buildGenericEffectHandler { effect ->
        when (effect) {
            is InputEffect.InputUpdate -> {
                holder.update { it.copy(outputText = effect.value) }
                delay(3.seconds)
                val selectedColor = queryData<ColorPickerUiState>("ColorPickerUiState")
                selectedColor ?: return@buildGenericEffectHandler
                holder.update {
                    it.copy(outputText = it.outputText + " and Selected color index is ${selectedColor.selectedColorIndex}")
                }
                publish(effect = RouterEffect(holder.getValue()))
                delay(1.seconds)
                send(
                    effect = ColorPickerEffect.BackgroundColorUpdate(holder2.getValue().backgroundColor),
                    to = ColorPickerTransformer::class
                )
            }

            is ColorPickerEffect.BackgroundColorUpdate -> {
                holder.update { it.copy(backgroundColor = effect.color) }
            }
        }
    }
}

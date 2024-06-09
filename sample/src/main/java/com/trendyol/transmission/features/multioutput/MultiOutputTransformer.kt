package com.trendyol.transmission.features.multioutput

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.output.OutputCalculationResult
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.ui.MultiOutputUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

class MultiOutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher) {

    private val holder = buildDataHolder(MultiOutputUiState())

    override val effectHandler = buildGenericEffectHandler { effect ->
        when (effect) {
            is InputEffect.InputUpdate -> {
                holder.update { it.copy(writtenUppercaseText = effect.value.uppercase()) }
                val result = queryComputation<OutputCalculationResult>("OutputCalculation")
                holder.update {
                    it.copy(writtenUppercaseText = it.writtenUppercaseText + " ${result?.result}")
                }
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

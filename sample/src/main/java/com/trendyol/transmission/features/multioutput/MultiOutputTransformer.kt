package com.trendyol.transmission.features.multioutput

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.output.OutputTransformer
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlerRegistry
import com.trendyol.transmission.transformer.handler.registerEffect
import com.trendyol.transmission.transformer.request.createIdentity
import com.trendyol.transmission.ui.MultiOutputUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val multiOutputTransformerIdentity = createIdentity("MultiOutput")

class MultiOutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher, multiOutputTransformerIdentity) {

    private val holder = buildDataHolder(MultiOutputUiState())

    override val handlerRegistry: HandlerRegistry = handlerRegistry {
        registerEffect<InputEffect.InputUpdate> { effect ->
            holder.update { it.copy(writtenUppercaseText = effect.value.uppercase()) }
            val result = compute(OutputTransformer.outputCalculationContract)
            holder.update {
                it.copy(writtenUppercaseText = it.writtenUppercaseText + " ${result?.result}")
            }
        }
        registerEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update { it.copy(backgroundColor = effect.color) }
        }
        registerEffect<ColorPickerEffect.SelectedColorUpdate> { effect ->
            holder.update { it.copy(selectedColor = effect.color) }
        }
    }

}

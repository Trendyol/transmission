package com.trendyol.transmission.features.multioutput

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.output.OutputTransformer
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.identity
import com.trendyol.transmission.ui.MultiOutputUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val multiOutputTransformerIdentity = Contracts.identity("MultiOutput")

class MultiOutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher, multiOutputTransformerIdentity) {

    private val holder = dataHolder(MultiOutputUiState())

    override val handlers: HandlerRegistry = handlers {
        onEffect<InputEffect.InputUpdate> { effect ->
            holder.update { it.copy(writtenUppercaseText = effect.value.uppercase()) }
            val result = compute(OutputTransformer.outputCalculationContract)
            holder.update {
                it.copy(writtenUppercaseText = it.writtenUppercaseText + " ${result?.result}")
            }
        }
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update { it.copy(backgroundColor = effect.color) }
        }
        onEffect<ColorPickerEffect.SelectedColorUpdate> { effect ->
            holder.update { it.copy(selectedColor = effect.color) }
        }
    }

}

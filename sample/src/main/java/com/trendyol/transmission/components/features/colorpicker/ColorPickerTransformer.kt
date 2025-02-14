package com.trendyol.transmission.components.features.colorpicker

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.register
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val colorPickerIdentity = Contract.identity()

class ColorPickerTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(colorPickerIdentity, defaultDispatcher) {

    private val holder = dataHolder(ColorPickerUiState(), holderContract)

    override val handlers: Handlers = handlers {
        register<ColorPickerSignal.SelectColor> { signal ->
            holder.update { it.copy(selectedColorIndex = signal.index) }
            publish(
                ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
            )
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(signal.selectedColor),
                identity = multiOutputTransformerIdentity
            )
        }
        register<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update {
                it.copy(backgroundColor = effect.color)
            }
        }
    }

    companion object {
        val holderContract = Contract.dataHolder<ColorPickerUiState>()
    }
}

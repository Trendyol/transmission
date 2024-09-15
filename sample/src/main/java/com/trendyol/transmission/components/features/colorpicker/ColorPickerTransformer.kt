package com.trendyol.transmission.components.features.colorpicker

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.components.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.dataHolder
import com.trendyol.transmission.transformer.request.identity
import com.trendyol.transmission.components.features.ColorPickerUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val colorPickerIdentity = Contracts.identity("ColorPicker")

class ColorPickerTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(colorPickerIdentity, defaultDispatcher) {

    private val holder = dataHolder(ColorPickerUiState(), holderContract)

    override val handlers: HandlerRegistry = handlers {
        onSignal<ColorPickerSignal.SelectColor> { signal ->
            holder.update { it.copy(selectedColorIndex = signal.index) }
            publish(
                ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
            )
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(signal.selectedColor),
                identity = multiOutputTransformerIdentity
            )
        }
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update {
                it.copy(backgroundColor = effect.color)
            }
        }
    }

    companion object {
        val holderContract = Contracts.dataHolder<ColorPickerUiState>("ColorPickerUiState")
    }
}

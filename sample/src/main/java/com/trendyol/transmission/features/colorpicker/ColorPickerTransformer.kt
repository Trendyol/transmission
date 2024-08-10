package com.trendyol.transmission.features.colorpicker

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.effect
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.signal
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.dataHolder
import com.trendyol.transmission.transformer.request.identity
import com.trendyol.transmission.ui.ColorPickerUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val colorPickerIdentity = Contracts.identity("ColorPicker")

class ColorPickerTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher, colorPickerIdentity) {

    private val holder = dataHolder(ColorPickerUiState(), holderContract)

    override val handlers: HandlerRegistry = handlers {
        signal<ColorPickerSignal.SelectColor> { signal ->
            holder.update { it.copy(selectedColorIndex = signal.index) }
            publish(
                ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
            )
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(signal.selectedColor),
                identity = multiOutputTransformerIdentity
            )
        }
        effect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update {
                it.copy(backgroundColor = effect.color)
            }
        }
    }

    companion object {
        val holderContract = Contracts.dataHolder<ColorPickerUiState>("ColorPickerUiState")
    }
}

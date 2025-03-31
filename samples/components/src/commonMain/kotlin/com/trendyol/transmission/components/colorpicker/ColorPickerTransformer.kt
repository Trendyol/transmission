package com.trendyol.transmission.components.colorpicker

import com.trendyol.transmission.components.ColorPickerUiState
import com.trendyol.transmission.components.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.addHandlers
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.CoroutineDispatcher

val colorPickerIdentity = Contract.identity()

class ColorPickerTransformer constructor(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(colorPickerIdentity, defaultDispatcher) {

    private val holder = dataHolder(ColorPickerUiState(), holderContract)

    init {
        addHandlers {
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
    }

    companion object {
        val holderContract = Contract.dataHolder<ColorPickerUiState>()
    }
}

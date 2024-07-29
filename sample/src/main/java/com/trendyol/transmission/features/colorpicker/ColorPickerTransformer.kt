package com.trendyol.transmission.features.colorpicker

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlerRegistry
import com.trendyol.transmission.transformer.handler.registerEffect
import com.trendyol.transmission.transformer.handler.registerSignal
import com.trendyol.transmission.transformer.request.buildDataContract
import com.trendyol.transmission.transformer.request.createIdentity
import com.trendyol.transmission.ui.ColorPickerUiState
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val colorPickerIdentity = createIdentity("ColorPicker")

class ColorPickerTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher, colorPickerIdentity) {

    private val holder = buildDataHolder(ColorPickerUiState(), holderContract)

    override val handlerRegistry: HandlerRegistry = handlerRegistry {
        registerSignal<ColorPickerSignal.SelectColor> { signal ->
            holder.update { it.copy(selectedColorIndex = signal.index) }
            publish(
                ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
            )
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(signal.selectedColor),
                identity = multiOutputTransformerIdentity
            )
        }
        registerEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update {
                it.copy(backgroundColor = effect.color)
            }
        }
    }

    companion object {
        val holderContract = buildDataContract<ColorPickerUiState>("ColorPickerUiState")
    }
}

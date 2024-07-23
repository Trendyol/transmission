package com.trendyol.transmission.features.colorpicker

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericSignalHandler
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

    override val signalHandler = buildGenericSignalHandler { signal ->
        when (signal) {
            is ColorPickerSignal.SelectColor -> {
                holder.update { it.copy(selectedColorIndex = signal.index) }
                publish(
                    ColorPickerEffect.BackgroundColorUpdate(signal.selectedColor.copy(alpha = 0.1f))
                )
                send(
                    effect = ColorPickerEffect.SelectedColorUpdate(signal.selectedColor),
                    identity = multiOutputTransformerIdentity
                )
            }
        }
    }

    override val effectHandler = buildGenericEffectHandler { effect ->
        when (effect) {
            is ColorPickerEffect.BackgroundColorUpdate -> {
                holder.update {
                    it.copy(backgroundColor = effect.color)
                }
            }
        }
    }

    companion object {
        val holderContract = buildDataContract<ColorPickerUiState>("ColorPickerUiState")
    }
}

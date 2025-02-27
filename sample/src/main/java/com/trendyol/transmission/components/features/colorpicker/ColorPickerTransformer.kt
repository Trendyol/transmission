package com.trendyol.transmission.components.features.colorpicker

import android.util.Log
import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.components.LoggingModule
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.module.TransformerModule
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerScope
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

val colorPickerIdentity = Contract.identity()

class ColorPickerTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(colorPickerIdentity, defaultDispatcher) {

    private val holder = dataHolder(ColorPickerUiState(), holderContract)

    init {
        applyModule(LoggingModule("ColorPicker"))
    }

    override val handlers: Handlers = handlers {
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
        val holderContract = Contract.dataHolder<ColorPickerUiState>()
    }
}


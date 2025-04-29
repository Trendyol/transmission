package com.trendyol.transmission.components.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.InputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class InputTransformer constructor(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(InputUiState(), holderContract)

    override val computations: Computations = computations {
        register(writtenInputContract) {
            delay(1.seconds)
            WrittenInput(holder.getValue().writtenText)
        }
        register(writtenInputWithArgs) {
            WrittenInput(it)
        }
    }


    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            holder.update { it.copy(writtenText = signal.value) }
            val color = pauseOn(colorCheckpoint)
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(color),
                identity = multiOutputTransformerIdentity
            )
            publish(effect = InputEffect.InputUpdate(signal.value))
        }
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            validate(colorCheckpoint, effect.color)
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    @OptIn(ExperimentalTransmissionApi::class)
    companion object {
        val writtenInputWithArgs = Contract.computationWithArgs<String, WrittenInput>()
        val writtenInputContract = Contract.computation<WrittenInput>()
        val holderContract = Contract.dataHolder<InputUiState>()
        val colorCheckpoint = Contract.checkpointWithArgs<Color>()
    }
}


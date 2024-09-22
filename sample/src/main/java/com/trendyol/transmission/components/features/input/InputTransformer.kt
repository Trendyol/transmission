package com.trendyol.transmission.components.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.features.InputUiState
import com.trendyol.transmission.components.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.features.multioutput.multiOutputTransformerIdentity
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.checkpointWithArgs
import com.trendyol.transmission.transformer.request.computation
import com.trendyol.transmission.transformer.request.computation.ComputationRegistry
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.computationWithArgs
import com.trendyol.transmission.transformer.request.dataHolder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class InputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(InputUiState(), holderContract)

    override val computations: ComputationRegistry = computations {
        register(writtenInputContract) {
            delay(1.seconds)
            WrittenInput(holder.getValue().writtenText)
        }
        register(writtenInputWithArgs) {
            WrittenInput(it)
        }
    }

    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: HandlerRegistry = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            holder.update { it.copy(writtenText = signal.value) }
            pauseOn(colorCheckpoint) { color ->
                send(
                    effect = ColorPickerEffect.SelectedColorUpdate(color),
                    identity = multiOutputTransformerIdentity
                )
                publish(effect = InputEffect.InputUpdate(signal.value))
            }
            pauseOn(colorCheckpoint, colorCheckpoint) {}
        }
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            validate(colorCheckpoint, effect.color)
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    @OptIn(ExperimentalTransmissionApi::class)
    companion object {
        val writtenInputWithArgs = Contracts.computationWithArgs<String, WrittenInput>()
        val writtenInputContract = Contracts.computation<WrittenInput>()
        val holderContract = Contracts.dataHolder<InputUiState>()
        val colorCheckpoint = Contracts.checkpointWithArgs<Color>()
    }
}

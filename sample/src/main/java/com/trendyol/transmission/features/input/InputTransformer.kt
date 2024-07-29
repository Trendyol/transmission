package com.trendyol.transmission.features.input

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlerRegistry
import com.trendyol.transmission.transformer.handler.registerEffect
import com.trendyol.transmission.transformer.handler.registerSignal
import com.trendyol.transmission.transformer.request.buildComputationContract
import com.trendyol.transmission.transformer.request.buildComputationContractWithArgs
import com.trendyol.transmission.transformer.request.buildDataContract
import com.trendyol.transmission.transformer.request.computation.registerComputation
import com.trendyol.transmission.ui.InputUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class InputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher) {

    private val holder = buildDataHolder(InputUiState(), holderContract)

    init {
        computationRegistry
            .registerComputation(writtenInputContract) {
                delay(1.seconds)
                WrittenInput(holder.getValue().writtenText)
            }.registerComputation(writtenInputWithArgs) {
                WrittenInput(it)
            }
    }

    override val handlerRegistry: HandlerRegistry = handlerRegistry {
        registerSignal<InputSignal.InputUpdate> { signal ->
            holder.update { it.copy(writtenText = signal.value) }
            publish(effect = InputEffect.InputUpdate(signal.value))
        }
        registerEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    companion object {
        val writtenInputWithArgs =
            buildComputationContractWithArgs<String, WrittenInput>("WrittenInputWithArgs")
        val writtenInputContract = buildComputationContract<WrittenInput>("WrittenInput")
        val holderContract = buildDataContract<InputUiState>("InputUiState")
    }
}

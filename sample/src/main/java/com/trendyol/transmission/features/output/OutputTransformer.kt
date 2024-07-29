package com.trendyol.transmission.features.output

import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.colorpicker.colorPickerIdentity
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlerRegistry
import com.trendyol.transmission.transformer.handler.registerEffect
import com.trendyol.transmission.transformer.request.buildComputationContract
import com.trendyol.transmission.transformer.request.buildExecutionContract
import com.trendyol.transmission.transformer.request.computation.registerComputation
import com.trendyol.transmission.transformer.request.execution.registerExecution
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import com.trendyol.transmission.ui.theme.Pink80
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class OutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(defaultDispatcher) {

    private val holder = buildDataHolder(OutputUiState())

    private val holder2 = buildDataHolder(ColorPickerUiState(), publishUpdates = false)

    init {
        computationRegistry
            .registerComputation(outputCalculationContract) {
                delay(2.seconds)
                val data = getData(ColorPickerTransformer.holderContract)?.selectedColorIndex
                val writtenOutput = compute(InputTransformer.writtenInputContract)
                val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
                OutputCalculationResult("result is $result with ($writtenOutput) and $data")
            }
        executionRegistry
            .registerExecution(outputExecutionContract) {
                delay(4.seconds)
                communicationScope.publish(ColorPickerEffect.BackgroundColorUpdate(Pink80))
            }
    }

    override val handlerRegistry: HandlerRegistry = handlerRegistry {
        registerEffect<InputEffect.InputUpdate> { effect ->
            holder.update { it.copy(outputText = effect.value) }
            delay(3.seconds)
            val selectedColor = getData(ColorPickerTransformer.holderContract)
            selectedColor ?: return@registerEffect
            holder.update {
                it.copy(outputText = it.outputText + " and Selected color index is ${selectedColor.selectedColorIndex}")
            }
            delay(1.seconds)
            send(
                effect = ColorPickerEffect.BackgroundColorUpdate(holder2.getValue().backgroundColor),
                identity = colorPickerIdentity
            )
            execute(outputExecutionContract)
            publish(effect = RouterEffect(holder.getValue()))
        }
        registerEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    companion object {
        val outputCalculationContract =
            buildComputationContract<OutputCalculationResult>("OutputCalculationResult")
        val outputExecutionContract =
            buildExecutionContract("outputExecutionContract")
    }
}

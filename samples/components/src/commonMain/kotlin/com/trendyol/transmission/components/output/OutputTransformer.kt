package com.trendyol.transmission.components.output

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.components.ColorPickerUiState
import com.trendyol.transmission.components.OutputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.colorpicker.colorPickerIdentity
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.input.InputTransformer
import com.trendyol.transmission.components.util.Logger
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.addComputations
import com.trendyol.transmission.transformer.addExecutions
import com.trendyol.transmission.transformer.addHandlers
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.execution.register
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class OutputTransformer(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(OutputUiState())

    private val holder2 = dataHolder(ColorPickerUiState(), publishUpdates = false)

    init {
        addComputations {
            register(outputCalculationContract) {
                delay(2.seconds)
                val data = getData(ColorPickerTransformer.holderContract)?.selectedColorIndex
                val writtenOutput = compute(InputTransformer.writtenInputContract)
                val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
                OutputCalculationResult("result is $result with ($writtenOutput) and $data")
            }
        }
        addExecutions {
            register(outputExecutionContract) {
                delay(4.seconds)
                communicationScope.publish(
                    ColorPickerEffect.BackgroundColorUpdate(
                        Color.Red.copy(
                            alpha = 0.2f
                        )
                    )
                )
                throw RuntimeException(
                    "This exception will be properly handled and caught " +
                            "inside of the onError() function"
                )
            }
        }
        addHandlers {
            onEffect<InputEffect.InputUpdate> { effect ->
                holder.update { it.copy(outputText = effect.value) }
                delay(3.seconds)
                val selectedColor = getData(ColorPickerTransformer.holderContract)
                selectedColor ?: return@onEffect
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
            onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
                holder.update { it.copy(backgroundColor = effect.color) }
            }
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        Logger.d(TAG, "onError: ${throwable.message}")
    }

    companion object {
        private const val TAG = "OutputTransformer"
        val outputCalculationContract = Contract.computation<OutputCalculationResult>()
        val outputExecutionContract = Contract.execution()
    }
}

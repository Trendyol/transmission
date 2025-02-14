package com.trendyol.transmission.components.features.output

import android.util.Log
import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.OutputUiState
import com.trendyol.transmission.components.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.features.colorpicker.colorPickerIdentity
import com.trendyol.transmission.components.features.input.InputEffect
import com.trendyol.transmission.components.features.input.InputTransformer
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.register
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.execution.Executions
import com.trendyol.transmission.transformer.request.execution.executions
import com.trendyol.transmission.transformer.request.execution.register
import com.trendyol.transmission.ui.theme.Pink80
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds


class OutputTransformer @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(OutputUiState())

    private val holder2 = dataHolder(ColorPickerUiState(), publishUpdates = false)

    override val computations: Computations = computations {
        register(outputCalculationContract) {
            delay(2.seconds)
            val data = getData(ColorPickerTransformer.holderContract)?.selectedColorIndex
            val writtenOutput = compute(InputTransformer.writtenInputContract)
            val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
            OutputCalculationResult("result is $result with ($writtenOutput) and $data")
        }
    }

    override val executions: Executions = executions {
        register(outputExecutionContract) {
            delay(4.seconds)
            communicationScope.publish(ColorPickerEffect.BackgroundColorUpdate(Pink80))
            throw RuntimeException(
                "This exception will be properly handled and caught " +
                        "inside of the onError() function"
            )
        }
    }

    override val handlers: Handlers = handlers {
        register<InputEffect.InputUpdate> { effect ->
            holder.update { it.copy(outputText = effect.value) }
            delay(3.seconds)
            val selectedColor = getData(ColorPickerTransformer.holderContract)
            selectedColor ?: return@register
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
        register<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        Log.e(TAG, "onError: ${throwable.localizedMessage}")
    }

    companion object {
        private const val TAG = "OutputTransformer"
        val outputCalculationContract = Contract.computation<OutputCalculationResult>()
        val outputExecutionContract = Contract.execution()
    }
}

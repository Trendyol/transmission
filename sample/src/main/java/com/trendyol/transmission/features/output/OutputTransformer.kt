package com.trendyol.transmission.features.output

import android.util.Log
import com.trendyol.transmission.DefaultDispatcher
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.colorpicker.colorPickerIdentity
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.computation
import com.trendyol.transmission.transformer.request.computation.ComputationRegistry
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.execution
import com.trendyol.transmission.transformer.request.execution.ExecutionRegistry
import com.trendyol.transmission.transformer.request.execution.executions
import com.trendyol.transmission.transformer.request.execution.register
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
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(OutputUiState())

    private val holder2 = dataHolder(ColorPickerUiState(), publishUpdates = false)

    override val computations: ComputationRegistry = computations {
        register(outputCalculationContract) {
            delay(2.seconds)
            val data = getData(ColorPickerTransformer.holderContract)?.selectedColorIndex
            val writtenOutput = compute(InputTransformer.writtenInputContract)
            val result = Random.nextInt(5, 15) * Random.nextInt(5, 15)
            OutputCalculationResult("result is $result with ($writtenOutput) and $data")
        }
    }

    override val executions: ExecutionRegistry = executions {
        register(outputExecutionContract) {
            delay(4.seconds)
            communicationScope.publish(ColorPickerEffect.BackgroundColorUpdate(Pink80))
            throw RuntimeException(
                "This exception will be properly handled and caught " +
                        "inside of the onError() function"
            )
        }
    }

    override val handlers: HandlerRegistry = handlers {
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

    override fun onError(throwable: Throwable) {
        super.onError(throwable)
        Log.e(TAG, "onError: ${throwable.localizedMessage}")
    }

    companion object {
        private const val TAG = "OutputTransformer"
        val outputCalculationContract = Contracts.computation<OutputCalculationResult>()
        val outputExecutionContract = Contracts.execution()
    }
}

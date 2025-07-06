package com.trendyol.transmission.components

import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.components.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.input.InputTransformer
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.router.asState
import com.trendyol.transmission.router.streamData
import com.trendyol.transmission.router.streamDataAsState
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmissionviewmodel.RouterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class ComponentViewModel(
    transformerList: List<Transformer>
) : RouterViewModel(transformerList.toSet()) {

    val inputUiState = streamOwner
        .streamData<InputUiState> { _transmissionList.value = listOf() }
        .asState(viewModelScope, InputUiState())

    val outputUiState = streamDataAsState(OutputUiState())
    val colorPickerUiState = streamDataAsState(ColorPickerUiState())
    val multiOutputUiState = streamDataAsState(MultiOutputUiState())

    private val _transmissionList = MutableStateFlow<List<String>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()

    override fun onProcessSignal(signal: Transmission.Signal) {
        super.onProcessSignal(signal)
        _transmissionList.update { it.plus("Signal: $signal") }
    }

    override fun onEffect(effect: Transmission.Effect) {
        viewModelScope.launch {
            _transmissionList.update { it.plus("Effect: $effect") }
            if (effect is RouterEffect) {
                when (effect.payload) {
                    is OutputUiState -> {
                        _transmissionList.update { it.plus("Generic Effect: $effect") }
                    }
                }
            }
            val inputData = queryHandler.getData(InputTransformer.holderContract)
            delay(1.seconds)
            val colorPicker = queryHandler.getData(ColorPickerTransformer.holderContract)
            _transmissionList.update { it.plus("Current InputData: $inputData") }
            _transmissionList.update { it.plus("Current ColorPickerData: $colorPicker") }
        }
    }

    override fun onData(data: Transmission.Data) {
        _transmissionList.update { it.plus("Data: $data") }
    }
}

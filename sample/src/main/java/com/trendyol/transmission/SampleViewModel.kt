package com.trendyol.transmission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputTransformer
import com.trendyol.transmission.router.toState
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.InputUiState
import com.trendyol.transmission.ui.MultiOutputUiState
import com.trendyol.transmission.ui.OutputUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class SampleViewModel @Inject constructor(
    private val transmissionRouter: TransmissionRouter
) : ViewModel() {

    val inputUiState = transmissionRouter.dataStream.toState(viewModelScope, InputUiState())
    val outputUiState = transmissionRouter.dataStream.toState(viewModelScope, OutputUiState())
    val colorPickerUiState =
        transmissionRouter.dataStream.toState(viewModelScope, ColorPickerUiState())
    val multiOutputUiState =
        transmissionRouter.dataStream.toState(viewModelScope, MultiOutputUiState())
    private val _transmissionList = MutableStateFlow<List<String>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                transmissionRouter.dataStream.collect(::onData)
            }
            launch {
                transmissionRouter.effectStream.collect(::onEffect)
            }
        }
    }

    fun processSignal(signal: Transmission.Signal) {
        transmissionRouter.processSignal(signal)
        _transmissionList.update { it.plus("Signal: $signal") }
    }

    private fun onEffect(effect: Transmission.Effect) = viewModelScope.launch {
        _transmissionList.update { it.plus("Effect: $effect") }
        if (effect is RouterEffect) {
            when (effect.payload) {
                is OutputUiState -> {
                    _transmissionList.update { it.plus("Generic Effect: $effect") }
                }
            }
        }
        val inputData = transmissionRouter.requestHelper.getData(InputTransformer.holderContract)
        delay(1.seconds)
        val colorPicker =
            transmissionRouter.requestHelper.getData(ColorPickerTransformer.holderContract)
        _transmissionList.update { it.plus("Current InputData: $inputData") }
        _transmissionList.update { it.plus("Current ColorPickerData: $colorPicker") }
    }

    private fun onData(data: Transmission.Data) {
        _transmissionList.update { it.plus("Data: $data") }
    }

    override fun onCleared() {
        transmissionRouter.clear()
    }

}

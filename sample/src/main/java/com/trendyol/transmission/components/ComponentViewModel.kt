package com.trendyol.transmission.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.components.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.features.input.InputTransformer
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.onEach
import com.trendyol.transmission.router.toState
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.InputUiState
import com.trendyol.transmission.components.features.MultiOutputUiState
import com.trendyol.transmission.components.features.OutputUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ComponentViewModel @Inject constructor(
    private val router: TransmissionRouter
) : ViewModel() {

    val inputUiState = router.dataStream
        .onEach<InputUiState> { _transmissionList.value = listOf() }
        .toState(viewModelScope, InputUiState())

    val outputUiState = router.dataStream.toState(viewModelScope, OutputUiState())
    val colorPickerUiState = router.dataStream.toState(viewModelScope, ColorPickerUiState())
    val multiOutputUiState = router.dataStream.toState(viewModelScope, MultiOutputUiState())

    private val _transmissionList = MutableStateFlow<List<String>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                router.dataStream.collect(::onData)
            }
            launch {
                router.effectStream.collect(::onEffect)
            }
        }
    }

    fun processSignal(signal: Transmission.Signal) {
        router.processSignal(signal)
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
        val inputData = router.requestHelper.getData(InputTransformer.holderContract)
        delay(1.seconds)
        val colorPicker =
            router.requestHelper.getData(ColorPickerTransformer.holderContract)
        _transmissionList.update { it.plus("Current InputData: $inputData") }
        _transmissionList.update { it.plus("Current ColorPickerData: $colorPicker") }
    }

    private fun onData(data: Transmission.Data) {
        _transmissionList.update { it.plus("Data: $data") }
    }

    override fun onCleared() {
        router.clear()
    }

}

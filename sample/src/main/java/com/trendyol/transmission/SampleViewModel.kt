package com.trendyol.transmission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.InputUiState
import com.trendyol.transmission.ui.MultiOutputUiState
import com.trendyol.transmission.ui.OutputUiState
import com.trendyol.transmission.ui.SampleScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SampleViewModel @Inject constructor(
	private val transmissionRouter: DefaultTransmissionRouter
) : ViewModel() {

	private val _uiState = MutableStateFlow(SampleScreenUiState())
	val uiState = _uiState.asStateFlow()

	private val _transmissionList = MutableStateFlow<List<String>>(emptyList())
	val transmissionList = _transmissionList.asStateFlow()

	init {
		viewModelScope.launch {
			transmissionRouter.initialize(onData = ::onData, onEffect = ::onEffect)
		}
	}

	fun processSignal(signal: Transmission.Signal) {
		transmissionRouter.processSignal(signal)
		_transmissionList.update { it.plus("Signal: $signal") }
	}

	fun onEffect(effect: Transmission.Effect) {
		_transmissionList.update { it.plus("Effect: $effect") }
		if (effect is RouterPayloadEffect) {
			when (effect.payload) {
				is OutputUiState -> {
					_transmissionList.update { it.plus("Generic Effect: $effect")}
				}
			}
		}
	}


	private fun onData(data: Transmission.Data) {
		when (data) {
			is InputUiState -> _uiState.update { it.copy(inputState = data) }
			is OutputUiState -> _uiState.update { it.copy(outputState = data) }
			is MultiOutputUiState -> _uiState.update { it.copy(multiOutputState = data) }
			is ColorPickerUiState -> _uiState.update { it.copy(colorPickerState = data) }
		}
		_transmissionList.update { it.plus("Data: $data") }
	}


	override fun onCleared() {
		transmissionRouter.clear()
	}

}

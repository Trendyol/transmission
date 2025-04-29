package com.trendyol.transmission.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.router.streamData
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CounterViewModel : ViewModel() {
    private val master = Holder()
    private val router = TransmissionRouter {
        (1..200).map { Worker(id = it.toString()) }.plus(master).run {
            this@TransmissionRouter.addTransformerSet(this.toSet())
            this@TransmissionRouter.setCapacity(Capacity.Custom(256))
        }
    }
    private val counter = atomic<Int>(0)
    private val _transmissionList = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()
    private val _areAllDistinct = MutableStateFlow("")
    val areAllDistinct = _areAllDistinct.asStateFlow()

    private var areAllDistinctJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            router.streamData().collect(::onData)
        }
    }

    fun processSignal(signal: Transmission.Signal) {
        router.process(signal)
        _areAllDistinct.tryEmit("Calculating")
        checkTransmissions()
        counter.update { it.plus(1) }
    }

    private fun checkTransmissions() {
        areAllDistinctJob?.cancel()
        areAllDistinctJob = viewModelScope.launch(Dispatchers.Default) {
            delay(2000L)
            val allValueList = _transmissionList.value.map { it.first.split(" ").last() }
            val calculation = allValueList.toSet().size == allValueList.size
            _areAllDistinct.tryEmit(calculation.toString())
            val duplicates = allValueList.groupingBy { it }.eachCount().filter { it.value > 1 }
            _transmissionList.update {
                it.map { entry ->
                    if (duplicates.contains(
                            entry.first.split(" ").last()
                        )
                    ) entry.first to true else entry
                }
            }

        }
    }

    private suspend fun onData(data: Transmission.Data) {
        if (data !is CounterData) return
        withContext(Dispatchers.Default) {
            _transmissionList.update { it.plus("Data: ${data.id}" to false) }
        }
    }
}

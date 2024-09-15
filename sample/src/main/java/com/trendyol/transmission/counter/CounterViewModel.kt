package com.trendyol.transmission.counter

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.builder.TransmissionRouterBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class CounterViewModel @Inject constructor() : ViewModel() {
    private val master = Holder()
    private val router = TransmissionRouterBuilder.build {
        (1..120).map { Worker(id = it.toString()) }.plus(master).run {
            this@build.addTransformerSet(this.toSet())
        }
    }
    val counter = AtomicInteger(0)
    private val _transmissionList = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()
    private val _areAllDistinct = MutableStateFlow("")
    val areAllDistinct = _areAllDistinct.asStateFlow()

    private var areAllDistinctJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            router.dataStream.collect(::onData)
        }
    }

    fun processSignal(signal: Transmission.Signal) {
        router.processSignal(signal)
        _areAllDistinct.tryEmit("Calculating")
        checkTransmissions()
        counter.addAndGet(1)
    }

    private fun checkTransmissions() {
        areAllDistinctJob?.cancel()
        areAllDistinctJob = viewModelScope.launch(Dispatchers.Default) {
            delay(2000L)
            val allValueList = _transmissionList.value.map { it.first.split(" ").last() }
            val calculation = allValueList.toSet().size == allValueList.size
            _areAllDistinct.tryEmit(calculation.toString())
            Log.d("Counter-Test", "Master is: ${master.counterData.getValue()}")
            Log.d("Counter-Test", "Clicked ${counter.get()} times")
            Log.d(
                "Counter-Test",
                "AllValueList: ${allValueList.size} Set: ${allValueList.toSet().size}"
            )
            val duplicates = allValueList.groupingBy { it }.eachCount().filter { it.value > 1 }
            Log.d("Counter-Test", "Duplicates are: $duplicates")
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

    private fun onData(data: Transmission.Data) {
        if (data !is CounterData) return
        _transmissionList.update { it.plus("Data: ${data.id}" to false) }
    }
}
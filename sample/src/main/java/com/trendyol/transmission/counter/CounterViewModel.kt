package com.trendyol.transmission.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.builder.TransmissionRouterBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@HiltViewModel
class CounterViewModel @Inject constructor() : ViewModel() {
    val master = Holder()
    val router = TransmissionRouterBuilder.build {
        (1..100).map { Worker(id = it.toString()) }.plus(master).run {
            this@build.addTransformerSet(this.toSet())
        }
    }
    val counter = AtomicInteger(0)
    private val _transmissionList = MutableStateFlow<List<String>>(emptyList())
    val transmissionList = _transmissionList.asStateFlow()
    val areAllDistinct =
        _transmissionList.map { it.map { it.split(" ").last() }.distinct().size == it.size }
            .flowOn(Dispatchers.Default)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            router.dataStream.collect(::onData)
        }
    }

    fun processSignal(signal: Transmission.Signal) {
        router.processSignal(signal)
        println("Clicked ${counter.addAndGet(1)} times")
    }

    private fun onData(data: Transmission.Data) {
        if (data !is CounterData) return
        _transmissionList.update { it.plus("Data: ${data.id}") }
        println("Master is: ${master.counterData.getValue()}")
    }
}
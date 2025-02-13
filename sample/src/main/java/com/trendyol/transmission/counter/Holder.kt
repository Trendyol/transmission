package com.trendyol.transmission.counter

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.createComputations
import com.trendyol.transmission.transformer.request.computation.register
import com.trendyol.transmission.transformer.request.computationWithArgs

val lookUpAndReturn = Contracts.computationWithArgs<String, Int>()

class Holder : Transformer() {

    data class TestCounter(val value: Int) : Transmission.Data

    val counterData = dataHolder(TestCounter(0))

    override val computations: Computations = createComputations {
        register(lookUpAndReturn) { id ->
            counterData.updateAndGet { it.copy(value = it.value.plus(1)) }.value
        }
    }
}

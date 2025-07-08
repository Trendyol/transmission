package com.trendyol.transmissiontest.computation

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal class ComputationTransformer<D : Any?>(
    contract: Contract.Computation<D>, data: () -> D
) : Transformer(dispatcher = UnconfinedTestDispatcher()) {
    override val computations: Computations = computations {
        register(contract) {
            data()
        }
    }
}

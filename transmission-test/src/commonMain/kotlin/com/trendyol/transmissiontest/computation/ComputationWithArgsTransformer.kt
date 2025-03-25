package com.trendyol.transmissiontest.computation

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal class ComputationWithArgsTransformer<C : Contract.ComputationWithArgs<A, D?>, D : Any?, A : Any>(
    contract: C, data: () -> D?
) : Transformer(dispatcher = UnconfinedTestDispatcher()) {
    override val computations: Computations = computations {
        register(contract) {
            data()
        }
    }
}

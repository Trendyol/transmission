package com.trendyol.transmissiontest.computation

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.computation.Computations
import com.trendyol.transmission.transformer.request.computation.computations
import com.trendyol.transmission.transformer.request.computation.register
import kotlinx.coroutines.CoroutineDispatcher

internal class ComputationTransformer<D : Any?>(
    contract: Contract.Computation<D>, data: () -> D,
    coroutineDispatcher: CoroutineDispatcher,
) : Transformer(dispatcher = coroutineDispatcher) {
    override val computations: Computations = computations {
        register(contract) {
            data()
        }
    }
}

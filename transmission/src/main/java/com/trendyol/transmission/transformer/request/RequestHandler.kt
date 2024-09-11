package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission

interface RequestHandler {

    suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D?

    suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean = false,
    ): D?

    suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean = false,
    ): D?

    suspend fun execute(contract: Contract.Execution)

    suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(contract: C, args: A)
}

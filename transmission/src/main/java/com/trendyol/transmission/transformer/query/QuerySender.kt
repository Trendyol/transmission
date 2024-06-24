package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

interface QuerySender {

    suspend fun <C : Contract.Data<D>, D : Transmission.Data> queryData(contract: C): D?

    suspend fun <C : Contract.Computation<D>, D : Transmission.Data> queryComputation(
        contract: C,
        invalidate: Boolean = false,
    ): D?

    suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Transmission.Data> queryComputationWithArgs(
        contract: C,
        args: A,
        invalidate: Boolean = false,
    ): D?
}

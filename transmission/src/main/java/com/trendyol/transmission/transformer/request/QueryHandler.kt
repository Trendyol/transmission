package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

interface QueryHandler {

    /**
     * Gets the data using the provided [Contract.DataHolder]
     * @param contract DataHolder Contract to be sent
     */
    suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D?

    /**
     * Starts computation in the target [Transformer] and returns the result data.
     * @param contract Computation Contract to be sent
     * @param invalidate if the Computation is cached, this invalidates the result make it compute
     * again. If it is not cached, it doesn't have any effect.
     */
    suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean = false,
    ): D?

    /**
     * Starts computation in the target [Transformer] and returns the result data.
     * @param contract Computation Contract to be sent
     * @param args data required by Computation
     * @param invalidate if the Computation is cached, this invalidates the result make it compute
     * again. If it is not cached, it doesn't have any effect.
     */
    suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean = false,
    ): D?

    /**
     * Starts Execution in the target [Transformer]
     * @param contract Execution Contract to be sent
     */
    suspend fun execute(contract: Contract.Execution)

    /**
     * Starts Execution in the target [Transformer]
     * @param contract Execution Contract to be sent
     * @param args data required by Execution
     */
    suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(contract: C, args: A)
}

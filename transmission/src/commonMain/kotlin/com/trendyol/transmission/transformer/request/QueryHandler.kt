package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

interface QueryHandler {

    /**
     * Gets the data using the provided [Contract.DataHolder]
     * @param contract DataHolder Contract to be sent
     */
    suspend fun <D : Transmission.Data> getData(contract: Contract.DataHolder<D>): D?

    /**
     * Starts computation in the target [Transformer] and returns the result data.
     * @param contract Computation Contract to be sent
     * @param invalidate if the Computation is cached, this invalidates the result make it compute
     * again. If it is not cached, it doesn't have any effect.
     */
    suspend fun <D : Any> compute(contract: Contract.Computation<D>, invalidate: Boolean = false): D?

    /**
     * Starts computation in the target [Transformer] and returns the result data.
     * @param contract Computation Contract to be sent
     * @param args data required by Computation
     * @param invalidate if the Computation is cached, this invalidates the result make it compute
     * again. If it is not cached, it doesn't have any effect.
     */
    suspend fun <A : Any, D : Any> compute(
        contract: Contract.ComputationWithArgs<A, D>,
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
    suspend fun <A : Any> execute(contract: Contract.ExecutionWithArgs<A>, args: A)
}

package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission

fun <T : Transmission.Data?> buildDataContract(
    key: String
) = object : Contract.Data<T>() {
    override val key: String = key
}

fun <A : Any> buildComputationContract(
    key: String,
    useCache: Boolean = false
) = object : Contract.Computation<A>() {
    override val key: String = key
    override val useCache: Boolean = useCache
}

fun <A : Any, T : Any> buildComputationContractWithArgs(
    key: String,
    useCache: Boolean = false
) = object : Contract.ComputationWithArgs<A, T>() {
    override val key: String = key
    override val useCache: Boolean = useCache
}

fun buildExecutionContract(
    key: String,
) = object : Contract.Execution() {
    override val key: String = key
}

fun <A : Any> buildExecutionContractWithArgs(
    key: String,
) = object : Contract.ExecutionWithArgs<A>() {
    override val key: String = key
}

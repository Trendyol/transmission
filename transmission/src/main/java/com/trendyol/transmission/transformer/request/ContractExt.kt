package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission

object Contracts

fun Contracts.identity(key: String): Contract.Identity = Contract.Identity(key)

fun <T : Transmission.Data?> Contracts.dataHolder(
    key: String
) = object : Contract.DataHolder<T>() {
    override val key: String = key
}

fun <A : Any> Contracts.computation(
    key: String,
    useCache: Boolean = false
) = object : Contract.Computation<A>() {
    override val key: String = key
    override val useCache: Boolean = useCache
}

fun <A : Any, T : Any> Contracts.computationWithArgs(
    key: String,
    useCache: Boolean = false
) = object : Contract.ComputationWithArgs<A, T>() {
    override val key: String = key
    override val useCache: Boolean = useCache
}

fun Contracts.execution(
    key: String,
) = object : Contract.Execution() {
    override val key: String = key
}

fun <A : Any> Contracts.executionWithArgs(
    key: String,
) = object : Contract.ExecutionWithArgs<A>() {
    override val key: String = key
}

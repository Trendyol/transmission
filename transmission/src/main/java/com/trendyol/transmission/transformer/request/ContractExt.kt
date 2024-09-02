@file:OptIn(ExperimentalUuidApi::class)

package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object Contracts

fun Contracts.identity(): Contract.Identity {
    return Contract.Identity(key = Uuid.random().toString())
}

fun <T : Transmission.Data?> Contracts.dataHolder(): Contract.DataHolder<T> {
    return Contract.DataHolder<T>(key = Uuid.random().toString())
}

fun <A : Any> Contracts.computation(
    useCache: Boolean = false
): Contract.Computation<A> {
    return Contract.Computation<A>(key = Uuid.random().toString(), useCache = useCache)
}

fun <A : Any, T : Any> Contracts.computationWithArgs(
    useCache: Boolean = false
): Contract.ComputationWithArgs<A, T> {
    return Contract.ComputationWithArgs<A, T>(key = Uuid.random().toString(), useCache = useCache)
}

fun Contracts.execution(): Contract.Execution {
    return Contract.Execution(key = Uuid.random().toString())
}

fun <A : Any> Contracts.executionWithArgs(): Contract.ExecutionWithArgs<A> {
    return Contract.ExecutionWithArgs<A>(key = Uuid.random().toString())
}

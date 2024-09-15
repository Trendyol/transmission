package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator

object Contracts

fun Contracts.identity(): Contract.Identity {
    return Contract.Identity(key = IdentifierGenerator.generateIdentifier())
}

fun <T : Transmission.Data?> Contracts.dataHolder(): Contract.DataHolder<T> {
    return Contract.DataHolder<T>(key = IdentifierGenerator.generateIdentifier())
}

fun <A : Any> Contracts.computation(
    useCache: Boolean = false
): Contract.Computation<A> {
    return Contract.Computation<A>(
        key = IdentifierGenerator.generateIdentifier(),
        useCache = useCache
    )
}

fun <A : Any, T : Any> Contracts.computationWithArgs(
    useCache: Boolean = false
): Contract.ComputationWithArgs<A, T> {
    return Contract.ComputationWithArgs<A, T>(
        key = IdentifierGenerator.generateIdentifier(),
        useCache = useCache
    )
}

fun Contracts.execution(): Contract.Execution {
    return Contract.Execution(key = IdentifierGenerator.generateIdentifier())
}

fun <A : Any> Contracts.executionWithArgs(): Contract.ExecutionWithArgs<A> {
    return Contract.ExecutionWithArgs<A>(key = IdentifierGenerator.generateIdentifier())
}

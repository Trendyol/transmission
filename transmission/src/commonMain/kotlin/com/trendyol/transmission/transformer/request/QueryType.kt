package com.trendyol.transmission.transformer.request

internal sealed interface QueryType {

    class Data(
        val sender: String,
        val key: String,
        val queryIdentifier: String,
    ) : QueryType

    class Computation(
        val sender: String,
        val key: String,
        val queryIdentifier: String,
        val invalidate: Boolean = false,
    ) : QueryType

    class ComputationWithArgs<A : Any>(
        val sender: String,
        val key: String,
        val args: A,
        val queryIdentifier: String,
        val invalidate: Boolean = false,
    ) : QueryType

    class Execution(
        val key: String,
    ) : QueryType

    class ExecutionWithArgs<A : Any>(
        val key: String,
        val args: A,
    ) : QueryType

}

package com.trendyol.transmission.transformer.request

internal sealed interface Query {

    class Data(
        val sender: String,
        val key: String,
        val queryIdentifier: String,
    ) : Query

    class Computation(
        val sender: String,
        val key: String,
        val queryIdentifier: String,
        val invalidate: Boolean = false,
    ) : Query

    class ComputationWithArgs<A : Any>(
        val sender: String,
        val key: String,
        val args: A,
        val queryIdentifier: String,
        val invalidate: Boolean = false,
    ) : Query

    class Execution(
        val key: String,
    ) : Query

    class ExecutionWithArgs<A : Any>(
        val key: String,
        val args: A,
    ) : Query
}

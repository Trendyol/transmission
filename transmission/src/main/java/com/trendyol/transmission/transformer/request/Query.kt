package com.trendyol.transmission.transformer.request

internal sealed interface Query {

    data class Data(
        val sender: String,
        val key: String,
    ) : Query

    data class Computation(
        val sender: String,
        val key: String,
        val invalidate: Boolean = false,
    ) : Query

    data class ComputationWithArgs<A : Any>(
        val sender: String,
        val key: String,
        val args: A,
        val invalidate: Boolean = false,
    ) : Query

    data class Execution(
        val sender: String,
        val key: String,
    ) : Query

    data class ExecutionWithArgs<A : Any>(
        val sender: String,
        val key: String,
        val args: A,
    ) : Query
}

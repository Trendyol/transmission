package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import kotlin.jvm.JvmInline

sealed interface Contract {

    class Identity internal constructor(internal val key: String) : Contract

    class DataHolder<T : Transmission.Data?> internal constructor(
        internal val key: String,
    ) : Contract

    class Computation<T : Any?> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    class ComputationWithArgs<A : Any, T : Any?> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    @JvmInline
    value class Execution internal constructor(internal val key: String) : Contract

    class ExecutionWithArgs<A : Any> internal constructor(
        internal val key: String
    ) : Contract

    sealed class Checkpoint(
        internal open val key: String,
    ) : Contract {
        class Default internal constructor(
            override val key: String,
        ) : Checkpoint(key)

        class WithArgs<A : Any> internal constructor(
            override val key: String,
        ) : Checkpoint(key)
    }

    companion object {

        fun identity(): Identity {
            return Identity(key = IdentifierGenerator.generateIdentifier())
        }

        fun <T : Transmission.Data?> dataHolder(): DataHolder<T> {
            return DataHolder<T>(key = IdentifierGenerator.generateIdentifier())
        }

        fun <A : Any?> computation(
            useCache: Boolean = false
        ): Computation<A> {
            return Computation<A>(
                key = IdentifierGenerator.generateIdentifier(),
                useCache = useCache
            )
        }

        fun <A : Any, T : Any?> computationWithArgs(
            useCache: Boolean = false
        ): ComputationWithArgs<A, T> {
            return ComputationWithArgs<A, T>(
                key = IdentifierGenerator.generateIdentifier(),
                useCache = useCache
            )
        }

        fun execution(): Execution {
            return Execution(key = IdentifierGenerator.generateIdentifier())
        }

        fun <A : Any> executionWithArgs(): ExecutionWithArgs<A> {
            return ExecutionWithArgs<A>(key = IdentifierGenerator.generateIdentifier())
        }

        @ExperimentalTransmissionApi
        fun checkpoint(): Checkpoint.Default {
            return Checkpoint.Default(
                key = IdentifierGenerator.generateIdentifier(),
            )
        }

        @ExperimentalTransmissionApi
        fun <A : Any> checkpointWithArgs(): Checkpoint.WithArgs<A> {
            return Checkpoint.WithArgs(
                key = IdentifierGenerator.generateIdentifier(),
            )
        }
    }
}

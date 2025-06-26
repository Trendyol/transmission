package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier.IdentifierGenerator
import kotlin.jvm.JvmInline

/**
 * Defines type-safe contracts for inter-transformer communication and resource identification.
 * 
 * Contracts serve as compile-time safe identifiers for various communication patterns within
 * the Transmission framework. They enable type-safe queries, computations, executions, and
 * data holder access between transformers.
 * 
 * Key contract types:
 * - [Identity]: Unique identifier for transformers
 * - [DataHolder]: Contract for accessing transformer-held data
 * - [Computation]: Contract for querying computed values
 * - [Execution]: Contract for triggering remote operations
 * - [Checkpoint]: Contract for checkpoint-based flow control
 * 
 * @see QueryHandler for using contracts in queries
 * @see dataHolder for using DataHolder contracts
 */
sealed interface Contract {

    /**
     * Unique identifier contract for transformers to enable targeted communication.
     * 
     * Identity contracts allow transformers to send effects directly to specific transformers
     * rather than broadcasting to all transformers in the system.
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val myTransformerIdentity = Contract.identity()
     * }
     * 
     * class MyTransformer : Transformer(identity = myTransformerIdentity) {
     *     // Transformer implementation
     * }
     * ```
     */
    class Identity internal constructor(internal val key: String) : Contract

    /**
     * Contract for accessing data held by transformers through their data holders.
     * 
     * DataHolder contracts enable other transformers to query the current state of
     * data holders in different transformers, providing a way to access shared state.
     * 
     * @param T The type of data held by the data holder
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val userStateContract = Contract.dataHolder<UserState>()
     * }
     * 
     * // In transformer A
     * private val userState = dataHolder(UserState.Empty, userStateContract)
     * 
     * // In transformer B
     * val currentUser = query(userStateContract)
     * ```
     */
    class DataHolder<T : Transmission.Data?> internal constructor(
        internal val key: String,
    ) : Contract

    /**
     * Contract for querying computed values from transformers.
     * 
     * Computation contracts allow transformers to expose calculations or data processing
     * that other transformers can request. Results can optionally be cached for performance.
     * 
     * @param T The return type of the computation
     * @param useCache Whether to cache computation results for improved performance
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val userCountContract = Contract.computation<Int>(useCache = true)
     * }
     * 
     * // In transformer A
     * override val computations = computations {
     *     register(userCountContract) {
     *         userRepository.getActiveUserCount()
     *     }
     * }
     * 
     * // In transformer B
     * val userCount = compute(userCountContract)
     * ```
     */
    class Computation<T : Any?> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    /**
     * Contract for querying computed values with arguments from transformers.
     * 
     * Similar to [Computation] but allows passing arguments to the computation function.
     * Useful for parameterized queries and calculations.
     * 
     * @param A The type of arguments passed to the computation
     * @param T The return type of the computation
     * @param useCache Whether to cache computation results for improved performance
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val userByIdContract = Contract.computationWithArgs<String, User?>()
     * }
     * 
     * // In transformer A
     * override val computations = computations {
     *     register(userByIdContract) { userId ->
     *         userRepository.findById(userId)
     *     }
     * }
     * 
     * // In transformer B
     * val user = compute(userByIdContract, "user123")
     * ```
     */
    class ComputationWithArgs<A : Any, T : Any?> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    /**
     * Contract for triggering operations on remote transformers without return values.
     * 
     * Execution contracts enable transformers to trigger side effects or operations
     * on other transformers. Unlike computations, executions don't return values.
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val clearCacheContract = Contract.execution()
     * }
     * 
     * // In transformer A
     * override val executions = executions {
     *     register(clearCacheContract) {
     *         cache.clear()
     *     }
     * }
     * 
     * // In transformer B
     * execute(clearCacheContract)
     * ```
     */
    @JvmInline
    value class Execution internal constructor(internal val key: String) : Contract

    /**
     * Contract for triggering operations with arguments on remote transformers.
     * 
     * Similar to [Execution] but allows passing arguments to the execution function.
     * Useful for parameterized operations and side effects.
     * 
     * @param A The type of arguments passed to the execution
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val sendNotificationContract = Contract.executionWithArgs<String>()
     * }
     * 
     * // In transformer A
     * override val executions = executions {
     *     register(sendNotificationContract) { message ->
     *         notificationService.send(message)
     *     }
     * }
     * 
     * // In transformer B
     * execute(sendNotificationContract, "Hello World!")
     * ```
     */
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

        /**
         * Creates a unique identity contract for transformer identification.
         * 
         * Identity contracts are used to uniquely identify transformers in the system,
         * enabling targeted communication between specific transformers.
         * 
         * @return A new [Identity] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val userTransformerId = Contract.identity()
         * }
         * 
         * class UserTransformer : Transformer(identity = userTransformerId)
         * ```
         */
        fun identity(): Identity {
            return Identity(key = IdentifierGenerator.generateIdentifier())
        }

        /**
         * Creates a data holder contract for accessing transformer-held data.
         * 
         * Data holder contracts enable inter-transformer queries of data held by other transformers.
         * Multiple transformers can share the same contract to access the same data holder.
         * 
         * @param T The type of data held by the data holder
         * @return A new [DataHolder] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val sharedUserStateContract = Contract.dataHolder<UserState>()
         * }
         * ```
         */
        fun <T : Transmission.Data?> dataHolder(): DataHolder<T> {
            return DataHolder<T>(key = IdentifierGenerator.generateIdentifier())
        }

        /**
         * Creates a computation contract for querying calculated values from transformers.
         * 
         * Computation contracts allow transformers to expose calculations that other
         * transformers can request. Results can optionally be cached for performance.
         * 
         * @param A The return type of the computation
         * @param useCache Whether to cache computation results for improved performance
         * @return A new [Computation] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val expensiveCalculationContract = Contract.computation<BigDecimal>(useCache = true)
         * }
         * ```
         */
        fun <A : Any?> computation(
            useCache: Boolean = false
        ): Computation<A> {
            return Computation<A>(
                key = IdentifierGenerator.generateIdentifier(),
                useCache = useCache
            )
        }

        /**
         * Creates a computation contract that accepts arguments for parameterized calculations.
         * 
         * Similar to [computation] but allows passing arguments to the computation function.
         * Useful for queries that need input parameters.
         * 
         * @param A The type of arguments passed to the computation
         * @param T The return type of the computation
         * @param useCache Whether to cache computation results for improved performance
         * @return A new [ComputationWithArgs] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val userSearchContract = Contract.computationWithArgs<String, List<User>>()
         * }
         * ```
         */
        fun <A : Any, T : Any?> computationWithArgs(
            useCache: Boolean = false
        ): ComputationWithArgs<A, T> {
            return ComputationWithArgs<A, T>(
                key = IdentifierGenerator.generateIdentifier(),
                useCache = useCache
            )
        }

        /**
         * Creates an execution contract for triggering operations on remote transformers.
         * 
         * Execution contracts enable transformers to trigger side effects or operations
         * on other transformers without expecting return values.
         * 
         * @return A new [Execution] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val triggerBackupContract = Contract.execution()
         * }
         * ```
         */
        fun execution(): Execution {
            return Execution(key = IdentifierGenerator.generateIdentifier())
        }

        /**
         * Creates an execution contract that accepts arguments for parameterized operations.
         * 
         * Similar to [execution] but allows passing arguments to the execution function.
         * Useful for operations that need input parameters.
         * 
         * @param A The type of arguments passed to the execution
         * @return A new [ExecutionWithArgs] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val deleteUserContract = Contract.executionWithArgs<String>()
         * }
         * ```
         */
        fun <A : Any> executionWithArgs(): ExecutionWithArgs<A> {
            return ExecutionWithArgs<A>(key = IdentifierGenerator.generateIdentifier())
        }

        /**
         * Creates a checkpoint contract for flow control within transformers.
         * 
         * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
         * 
         * Checkpoint contracts enable pause-and-resume flow control within transformer handlers,
         * allowing complex asynchronous coordination between transformers.
         * 
         * @return A new [Checkpoint.Default] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val validationCheckpoint = Contract.checkpoint()
         * }
         * ```
         */
        @ExperimentalTransmissionApi
        fun checkpoint(): Checkpoint.Default {
            return Checkpoint.Default(
                key = IdentifierGenerator.generateIdentifier(),
            )
        }

        /**
         * Creates a checkpoint contract with arguments for parameterized flow control.
         * 
         * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
         * 
         * Similar to [checkpoint] but allows passing arguments to the checkpoint validation function.
         * 
         * @param A The type of arguments used in checkpoint validation
         * @return A new [Checkpoint.WithArgs] contract with a unique identifier
         * 
         * Example usage:
         * ```kotlin
         * companion object {
         *     val dataValidationCheckpoint = Contract.checkpointWithArgs<ValidationData>()
         * }
         * ```
         */
        @ExperimentalTransmissionApi
        fun <A : Any> checkpointWithArgs(): Checkpoint.WithArgs<A> {
            return Checkpoint.WithArgs(
                key = IdentifierGenerator.generateIdentifier(),
            )
        }
    }
}

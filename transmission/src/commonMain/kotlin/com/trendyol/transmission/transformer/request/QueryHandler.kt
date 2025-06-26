package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

/**
 * Interface for querying data and executing operations across transformers.
 * 
 * QueryHandler provides the core inter-transformer communication capabilities, allowing
 * transformers to request data, trigger computations, and execute operations on other
 * transformers through type-safe contracts.
 * 
 * Key capabilities:
 * - Query data from data holders in other transformers
 * - Trigger computations and receive results
 * - Execute operations on remote transformers
 * - Support for both cached and uncached operations
 * - Type-safe parameter passing and result handling
 * 
 * @see Contract for creating query contracts
 * @see CommunicationScope for using queries within handlers
 */
interface QueryHandler {

    /**
     * Retrieves data from a data holder in another transformer.
     * 
     * This method queries the current value of a data holder identified by the provided
     * contract. The contract must be shared between the transformer holding the data
     * and the transformer making the query.
     * 
     * @param D The type of data held by the data holder
     * @param contract The data holder contract identifying which data to retrieve
     * @return The current data value, or null if not available
     * 
     * Example usage:
     * ```kotlin
     * // Shared contract
     * companion object {
     *     val userStateContract = Contract.dataHolder<UserState>()
     * }
     * 
     * // In transformer A (data holder)
     * private val userState = dataHolder(UserState.Empty, userStateContract)
     * 
     * // In transformer B (query)
     * val currentUser = getData(userStateContract)
     * if (currentUser?.isLoggedIn == true) {
     *     // Handle logged in state
     * }
     * ```
     * 
     * @see Contract.dataHolder for creating data holder contracts
     * @see com.trendyol.transmission.transformer.dataholder.dataHolder for creating data holders
     */
    suspend fun <D : Transmission.Data> getData(contract: Contract.DataHolder<D>): D?

    /**
     * Triggers a computation in another transformer and returns the result.
     * 
     * This method executes a computation registered with the provided contract in the
     * target transformer. If the computation is configured for caching, subsequent calls
     * will return cached results unless invalidated.
     * 
     * @param D The return type of the computation
     * @param contract The computation contract identifying which computation to execute
     * @param invalidate Whether to invalidate cached results and force recomputation
     * @return The computation result, or null if not available
     * 
     * Example usage:
     * ```kotlin
     * // Shared contract
     * companion object {
     *     val expensiveCalculationContract = Contract.computation<BigDecimal>(useCache = true)
     * }
     * 
     * // In transformer A (computation provider)
     * override val computations = computations {
     *     register(expensiveCalculationContract) {
     *         performExpensiveCalculation()
     *     }
     * }
     * 
     * // In transformer B (computation consumer)
     * val result = compute(expensiveCalculationContract)
     * val freshResult = compute(expensiveCalculationContract, invalidate = true)
     * ```
     * 
     * @see Contract.computation for creating computation contracts
     */
    suspend fun <D : Any> compute(contract: Contract.Computation<D>, invalidate: Boolean = false): D?

    /**
     * Triggers a computation with arguments in another transformer and returns the result.
     * 
     * Similar to [compute] but allows passing arguments to the computation function.
     * This enables parameterized computations and queries that depend on input data.
     * 
     * @param A The type of arguments passed to the computation
     * @param D The return type of the computation
     * @param contract The computation contract identifying which computation to execute
     * @param args The arguments to pass to the computation
     * @param invalidate Whether to invalidate cached results and force recomputation
     * @return The computation result, or null if not available
     * 
     * Example usage:
     * ```kotlin
     * // Shared contract
     * companion object {
     *     val userSearchContract = Contract.computationWithArgs<String, List<User>>()
     * }
     * 
     * // In transformer A (search provider)
     * override val computations = computations {
     *     register(userSearchContract) { query ->
     *         userRepository.search(query)
     *     }
     * }
     * 
     * // In transformer B (search consumer)
     * val users = compute(userSearchContract, "john")
     * val freshResults = compute(userSearchContract, "jane", invalidate = true)
     * ```
     * 
     * @see Contract.computationWithArgs for creating computation contracts with arguments
     */
    suspend fun <A : Any, D : Any> compute(
        contract: Contract.ComputationWithArgs<A, D>,
        args: A,
        invalidate: Boolean = false,
    ): D?

    /**
     * Executes an operation in another transformer without expecting a return value.
     * 
     * This method triggers an execution registered with the provided contract in the
     * target transformer. Unlike computations, executions are used for side effects
     * and don't return values.
     * 
     * @param contract The execution contract identifying which operation to execute
     * 
     * Example usage:
     * ```kotlin
     * // Shared contract
     * companion object {
     *     val clearCacheContract = Contract.execution()
     * }
     * 
     * // In transformer A (operation provider)
     * override val executions = executions {
     *     register(clearCacheContract) {
     *         cache.clear()
     *         logger.info("Cache cleared")
     *     }
     * }
     * 
     * // In transformer B (operation trigger)
     * execute(clearCacheContract) // Triggers cache clearing
     * ```
     * 
     * @see Contract.execution for creating execution contracts
     */
    suspend fun execute(contract: Contract.Execution)

    /**
     * Executes an operation with arguments in another transformer.
     * 
     * Similar to [execute] but allows passing arguments to the execution function.
     * This enables parameterized operations that depend on input data.
     * 
     * @param A The type of arguments passed to the execution
     * @param contract The execution contract identifying which operation to execute
     * @param args The arguments to pass to the execution
     * 
     * Example usage:
     * ```kotlin
     * // Shared contract
     * companion object {
     *     val sendNotificationContract = Contract.executionWithArgs<NotificationData>()
     * }
     * 
     * // In transformer A (notification provider)
     * override val executions = executions {
     *     register(sendNotificationContract) { notificationData ->
     *         notificationService.send(notificationData)
     *     }
     * }
     * 
     * // In transformer B (notification trigger)
     * execute(sendNotificationContract, NotificationData("Hello", "Welcome!"))
     * ```
     * 
     * @see Contract.executionWithArgs for creating execution contracts with arguments
     */
    suspend fun <A : Any> execute(contract: Contract.ExecutionWithArgs<A>, args: A)
}

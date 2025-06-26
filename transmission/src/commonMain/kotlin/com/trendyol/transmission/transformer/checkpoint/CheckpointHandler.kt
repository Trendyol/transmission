package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract

/**
 * Interface for checkpoint-based flow control within transformer handlers.
 * 
 * ⚠️ **Experimental API**: This entire interface is experimental and may change or be removed in future versions.
 * 
 * CheckpointHandler provides sophisticated flow control mechanisms that allow transformers to pause
 * and resume processing based on external validation. This enables complex coordination patterns
 * between transformers, such as waiting for user confirmation, external service responses, or
 * multi-stage validation processes.
 * 
 * Key capabilities:
 * - Pause signal/effect processing at specific points
 * - Wait for external validation before continuing
 * - Support for both parameterized and parameter-less checkpoints
 * - Multiple checkpoint coordination
 * - Resume processing from validation points
 * 
 * @see Contract.Checkpoint for creating checkpoint contracts
 * @see CommunicationScope for using checkpoints within handlers
 */
interface CheckpointHandler {

    /**
     * Pauses the current handler execution until the specified checkpoint is validated.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method suspends the current signal or effect processing until another part of the system
     * calls [validate] with the same checkpoint contract. This enables complex coordination patterns
     * where processing needs to wait for external validation or user interaction.
     * 
     * @param contract The checkpoint contract to wait for validation
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val userConfirmationCheckpoint = Contract.checkpoint()
     * }
     * 
     * // In signal handler
     * onSignal<DangerousOperation> { signal ->
     *     // Pause until user confirms
     *     pauseOn(userConfirmationCheckpoint)
     *     
     *     // Continue after validation
     *     performDangerousOperation()
     * }
     * 
     * // In another part of the system (e.g., UI confirmation)
     * validate(userConfirmationCheckpoint) // Resumes processing
     * ```
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint.Default,
    )

    /**
     * Pauses the current handler execution until all specified checkpoints are validated.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method suspends execution until all provided checkpoint contracts are validated.
     * All checkpoints must be validated before processing continues.
     * 
     * @param contract The checkpoint contracts to wait for validation
     * @throws IllegalStateException when no checkpoint contracts are provided
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val adminApprovalCheckpoint = Contract.checkpoint()
     *     val securityCheckCheckpoint = Contract.checkpoint()
     * }
     * 
     * // In signal handler
     * onSignal<SensitiveOperation> { signal ->
     *     // Wait for both admin approval and security check
     *     pauseOn(adminApprovalCheckpoint, securityCheckCheckpoint)
     *     
     *     // Continue after both validations
     *     performSensitiveOperation()
     * }
     * ```
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint.Default,
    )

    /**
     * Pauses the current handler execution until the specified checkpoint is validated with arguments.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method suspends execution until the checkpoint is validated with arguments, which are then
     * returned to the caller. This enables passing data from the validation point back to the
     * suspended handler.
     * 
     * @param A The type of arguments expected from checkpoint validation
     * @param contract The checkpoint contract to wait for validation
     * @return The arguments provided during validation
     * 
     * Example usage:
     * ```kotlin
     * companion object {
     *     val amountConfirmationCheckpoint = Contract.checkpointWithArgs<BigDecimal>()
     * }
     * 
     * // In signal handler
     * onSignal<PaymentRequest> { signal ->
     *     // Pause until user confirms amount
     *     val confirmedAmount = pauseOn(amountConfirmationCheckpoint)
     *     
     *     // Continue with confirmed amount
     *     processPayment(confirmedAmount)
     * }
     * 
     * // In UI confirmation handler
     * validate(amountConfirmationCheckpoint, userConfirmedAmount)
     * ```
     */
    @ExperimentalTransmissionApi
    suspend fun <A : Any> CommunicationScope.pauseOn(
        contract: Contract.Checkpoint.WithArgs<A>,
    ): A

    /**
     * Validates a checkpoint and resumes any paused execution waiting on it.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method validates the specified checkpoint contract, causing any handlers suspended
     * on [pauseOn] with the same contract to resume execution.
     * 
     * @param contract The checkpoint contract to validate
     * 
     * Example usage:
     * ```kotlin
     * // Resume execution paused on this checkpoint
     * validate(userConfirmationCheckpoint)
     * ```
     */
    @ExperimentalTransmissionApi
    suspend fun validate(contract: Contract.Checkpoint.Default)

    /**
     * Validates a checkpoint with arguments and resumes any paused execution waiting on it.
     * 
     * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
     * 
     * This method validates the specified checkpoint contract with arguments, causing any handlers
     * suspended on [pauseOn] with the same contract to resume execution and receive the arguments.
     * 
     * @param A The type of arguments to provide during validation
     * @param contract The checkpoint contract to validate
     * @param args The arguments to provide to the resumed handler
     * 
     * Example usage:
     * ```kotlin
     * // Resume execution with validation data
     * validate(amountConfirmationCheckpoint, userConfirmedAmount)
     * ```
     */
    @ExperimentalTransmissionApi
    suspend fun <A : Any> validate(contract: Contract.Checkpoint.WithArgs<A>, args: A)
}

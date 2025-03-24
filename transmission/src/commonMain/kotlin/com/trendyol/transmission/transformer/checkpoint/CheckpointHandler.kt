package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract

interface CheckpointHandler {

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.Checkpoint].
     * @param contract Checkpoint to check for pause condition
     * @return Unit after the [contract] is validated
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint.Default,
    )

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.Checkpoint]s.
     * @param contract Checkpoints to check for pause condition.
     * @return Unit after the [contract]s are validated
     * @throws IllegalStateException when no [Contract.Checkpoint] is supplied
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint.Default,
    )

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.CheckpointWithArgs]s.
     * @param contract Checkpoint with Args to check for pause condition.
     * @return the type of argument depicted in [contract] after the checkpoint is validated
     */
    @ExperimentalTransmissionApi
    suspend fun <C : Contract.Checkpoint.WithArgs<A>, A : Any> CommunicationScope.pauseOn(
        contract: C,
    ): A

    /**
     * Validates the given [Contract.Checkpoint] and resumes the execution added with [pauseOn]
     */
    @ExperimentalTransmissionApi
    suspend fun validate(contract: Contract.Checkpoint.Default)

    /**
     * Validates the given [Contract.CheckpointWithArgs] and resumes the execution added with [pauseOn]
     */
    @ExperimentalTransmissionApi
    suspend fun <C : Contract.Checkpoint.WithArgs<A>, A : Any> validate(contract: C, args: A)
}

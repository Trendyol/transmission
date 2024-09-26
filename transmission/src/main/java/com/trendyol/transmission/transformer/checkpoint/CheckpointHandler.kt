package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract

interface CheckpointHandler {

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.Checkpoint].
     * @param contract Checkpoint to check for pause condition
     * @param resumeBlock execution block that will run after [contract] is validated
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    )

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.Checkpoint]s.
     * @param contract Checkpoints to check for pause condition.
     * @param resumeBlock execution block that will run after [contract] is validated
     * @throws IllegalStateException when [Contract.Checkpoint]s have different frequency
     * @throws IllegalStateException when no [Contract.Checkpoint] is supplied
     */
    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    )

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.CheckpointWithArgs]s.
     * @param contract Checkpoint with Args to check for pause condition.
     * @param resumeBlock execution block that will run after [contract] is validated. It accepts
     * the type of the [Contract.CheckpointWithArgs] as argument.
     */
    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> CommunicationScope.pauseOn(
        contract: C,
        resumeBlock: suspend CommunicationScope.(args: A) -> Unit
    )

    /**
     * Pauses the processing of a [Transmission.Signal] or [Transmission.Effect] using
     * the given [Contract.CheckpointWithArgs]s.
     * @param contract Checkpoint with Args to check for pause condition.
     * @param contract2 Second Checkpoint with Args to check for pause condition.
     * @param resumeBlock execution block that will run after both [contract] and [contract2] is
     * validated. It accepts the type of the [Contract.CheckpointWithArgs] as arguments.
     */
    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, C2 : Contract.CheckpointWithArgs<B>, A : Any, B : Any> CommunicationScope.pauseOn(
        contract: C,
        contract2: C2,
        resumeBlock: suspend CommunicationScope.(A, B) -> Unit
    )

    /**
     * Validates the given [Contract.Checkpoint] and resumes the execution added with [pauseOn]
     */
    @ExperimentalTransmissionApi
    suspend fun validate(contract: Contract.Checkpoint)

    /**
     * Validates the given [Contract.CheckpointWithArgs] and resumes the execution added with [pauseOn]
     */
    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> validate(contract: C, args: A)
}

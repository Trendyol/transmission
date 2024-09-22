package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract

interface CheckpointHandler {

    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    )

    @ExperimentalTransmissionApi
    suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    )

    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> CommunicationScope.pauseOn(
        contract: C,
        resumeBlock: suspend CommunicationScope.(args: A) -> Unit
    )

    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, C2 : Contract.CheckpointWithArgs<B>, A : Any, B : Any> CommunicationScope.pauseOn(
        contract: C,
        contract2: C2,
        resumeBlock: suspend CommunicationScope.(A, B) -> Unit
    )

    @ExperimentalTransmissionApi
    suspend fun validate(contract: Contract.Checkpoint)

    @ExperimentalTransmissionApi
    suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> validate(contract: C, args: A)
}


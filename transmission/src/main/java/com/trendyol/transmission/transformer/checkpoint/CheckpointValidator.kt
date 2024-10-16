package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.InternalTransmissionApi
import com.trendyol.transmission.transformer.request.Contract

@InternalTransmissionApi
interface CheckpointValidator<C : Contract.Checkpoint, A : Any> {
    suspend fun validate(contract: C, args: A): Boolean
}
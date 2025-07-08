package com.trendyol.transmissiontest.checkpoint

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.request.Contract

data object DefaultCheckPoint : Transmission.Signal

data class CheckpointWithArgs<A : Any>(
    val checkpoint: Contract.Checkpoint.WithArgs<A>,
) : Transmission.Signal

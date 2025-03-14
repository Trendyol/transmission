package com.trendyol.transmissiontest.checkpoint

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.request.Contract

data object DefaultCheckPoint : Transmission.Signal

data class CheckpointWithArgs<C : Contract.Checkpoint.WithArgs<A>, A : Any>(
    val checkpoint: C,
) : Transmission.Signal

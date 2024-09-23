package com.trendyol.transmission.transformer.checkpoint

import com.trendyol.transmission.transformer.request.Contract

/**
 * Indicator of how frequent [Contract.Checkpoint] and [Contract.CheckpointWithArgs] will be
 * validated.
 */
sealed interface Frequency {
    /**
     * Validates the checkpoint only once
     */
    data object Once : Frequency

    /**
     * Validates the checkpoint each time execution encounters
     */
    data object Continuous : Frequency
}

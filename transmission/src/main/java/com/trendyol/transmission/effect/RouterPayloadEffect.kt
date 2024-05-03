package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer

/**
 * This effect does not get published to other [Transformer]s.
 * Use it to send payload directly to [TransmissionRouter].
 */
data class RouterPayloadEffect(val payload: Any): Transmission.Effect

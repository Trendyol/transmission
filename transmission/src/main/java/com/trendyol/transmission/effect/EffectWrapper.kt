package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.request.Contract

internal data class EffectWrapper(
    val effect: Transmission.Effect,
    val identity: Contract.Identity? = null
)

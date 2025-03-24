package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission

@PublishedApi
internal data class RouterEffectWithType<T : Any>(val payload: T) : Transmission.Effect
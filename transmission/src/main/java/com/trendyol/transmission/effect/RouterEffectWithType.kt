package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission

data class RouterEffectWithType<T: Any>(val payload: T): Transmission.Effect
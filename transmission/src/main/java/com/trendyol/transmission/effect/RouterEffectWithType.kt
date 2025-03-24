package com.trendyol.transmission.effect

import com.trendyol.transmission.InternalTransmissionApi
import com.trendyol.transmission.Transmission

@InternalTransmissionApi
data class RouterEffectWithType<T: Any>(val payload: T): Transmission.Effect
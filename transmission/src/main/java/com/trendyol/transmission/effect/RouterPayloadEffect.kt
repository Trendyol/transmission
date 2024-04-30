package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission

data class RouterPayloadEffect(val payload: Any): Transmission.Effect

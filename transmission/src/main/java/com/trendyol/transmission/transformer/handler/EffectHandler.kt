package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

fun interface EffectHandler {
	suspend fun onEffect(effect: Transmission.Effect)
}

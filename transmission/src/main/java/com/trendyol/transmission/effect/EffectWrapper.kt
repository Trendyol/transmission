package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

data class EffectWrapper(
	val effect: Transmission.Effect,
	val receiver: KClass<out Transformer>? = null
)

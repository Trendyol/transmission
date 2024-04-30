package com.trendyol.transmission.effect

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

data class EffectWrapper<E : Transmission.Effect, D: Transmission.Data,T : Transformer<D, E>>(
	val effect: E,
	val to: KClass<out T>? = null
)

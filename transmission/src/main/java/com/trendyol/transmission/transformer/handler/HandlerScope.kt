package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

interface HandlerScope<D: Transmission.Data, E: Transmission.Effect> {
	fun publishData(data: D?)
	fun publishEffect(effect: E)
	fun sendEffect(effect: E, to: KClass<out Transformer<D, E>>)
}

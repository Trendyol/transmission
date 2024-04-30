package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

fun interface EffectHandler<D : Transmission.Data, E : Transmission.Effect> {
	suspend fun CommunicationScope<D, E>.onEffect(effect: Transmission.Effect)
}

fun <D : Transmission.Data, E : Transmission.Effect> Transformer<D, E>.buildGenericEffectHandler(
	onEffect: suspend CommunicationScope<D, E>.(effect: Transmission.Effect) -> Unit
): EffectHandler<D, E> {
	return EffectHandler { effect -> onEffect(effect) }
}

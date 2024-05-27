package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

fun interface EffectHandler {
	suspend fun CommunicationScope.onEffect(effect: Transmission.Effect)
}

fun  buildGenericEffectHandler(
	onEffect: suspend CommunicationScope.(effect: Transmission.Effect) -> Unit
): EffectHandler {
	return EffectHandler { effect -> onEffect(effect) }
}


inline fun <reified HE : Transmission.Effect> buildTypedEffectHandler(
	crossinline onEffect: suspend CommunicationScope.(effect: HE) -> Unit
): EffectHandler {
	return EffectHandler { incomingEffect ->
		incomingEffect.takeIf { it is HE }?.let { effect -> onEffect(effect as HE) }
	}
}

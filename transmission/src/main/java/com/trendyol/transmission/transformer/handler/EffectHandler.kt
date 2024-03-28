package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

fun interface EffectHandler {
	suspend fun HandlerScope.onEffect(effect: Transmission.Effect)
}

fun buildGenericEffectHandler(
	onEffect: HandlerScope.(effect: Transmission.Effect) -> Unit
): EffectHandler {
	return EffectHandler { effect -> onEffect(effect) }
}

inline fun <reified E : Transmission.Effect> buildTypedEffectHandler(
	crossinline onEffect: suspend HandlerScope.(effect: E) -> Unit
): EffectHandler {
	return EffectHandler { incomingEffect ->
		incomingEffect
			.takeIf { it is E }
			?.let { effect -> onEffect(effect as E) }
	}
}

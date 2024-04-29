package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

fun interface EffectHandler<D: Transmission.Data> {
	suspend fun HandlerScope<D>.onEffect(effect: Transmission.Effect)
}

fun<D: Transmission.Data> Transformer<D>.buildGenericEffectHandler(
	onEffect: HandlerScope<D>.(effect: Transmission.Effect) -> Unit
): EffectHandler<D> {
	return EffectHandler { effect -> onEffect(effect) }
}

inline fun <D: Transmission.Data, reified E : Transmission.Effect> Transformer<D>.buildTypedEffectHandler(
	crossinline onEffect: suspend HandlerScope<D>.(effect: E) -> Unit
): EffectHandler<D> {
	return EffectHandler { incomingEffect ->
		incomingEffect
			.takeIf { it is E }
			?.let { effect -> onEffect(effect as E) }
	}
}

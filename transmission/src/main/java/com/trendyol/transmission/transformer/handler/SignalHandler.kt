package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

fun interface SignalHandler<D : Transmission.Data, E : Transmission.Effect> {
	suspend fun HandlerScope<D, E>.onSignal(signal: Transmission.Signal)
}

fun <D : Transmission.Data, E : Transmission.Effect> Transformer<D, E>.buildGenericSignalHandler(
	onSignal: suspend HandlerScope<D, E>.(signal: Transmission.Signal) -> Unit
): SignalHandler<D, E> {
	return SignalHandler { signal -> onSignal(signal) }
}

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

fun interface SignalHandler<D: Transmission.Data> {
	suspend fun HandlerScope<D>.onSignal(signal: Transmission.Signal)
}

fun<D: Transmission.Data> Transformer<D>.buildGenericSignalHandler(
	onSignal: suspend HandlerScope<D>.(signal: Transmission.Signal) -> Unit
): SignalHandler<D> {
	return SignalHandler { signal -> onSignal(signal) }
}

inline fun <D: Transmission.Data, reified S : Transmission.Signal> buildTypedSignalHandler(
	crossinline onSignal: suspend HandlerScope<D>.(signal: S) -> Unit
): SignalHandler<D> {
	return SignalHandler { incomingSignal ->
		incomingSignal
			.takeIf { it is S }
			?.let { signal -> onSignal(signal as S) }
	}
}

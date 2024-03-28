package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

fun interface SignalHandler {
	suspend fun onSignal(signal: Transmission.Signal)
}

fun buildGenericSignalHandler(
	onSignal: suspend (signal: Transmission.Signal) -> Unit
): SignalHandler {
	return SignalHandler { signal -> onSignal(signal) }
}

inline fun <reified S : Transmission.Signal> buildTypedSignalHandler(
	crossinline onSignal: suspend (signal: S) -> Unit
): SignalHandler {
	return SignalHandler { incomingSignal ->
		incomingSignal
			.takeIf { it is S }
			?.let { signal -> onSignal(signal as S) }
	}
}

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

fun interface SignalHandler {
	suspend fun CommunicationScope.onSignal(signal: Transmission.Signal)
}

fun buildGenericSignalHandler(
	onSignal: suspend CommunicationScope.(signal: Transmission.Signal) -> Unit
): SignalHandler {
	return SignalHandler { signal -> onSignal(signal) }
}

inline fun <reified S : Transmission.Signal> buildTypedSignalHandler(
	crossinline onSignal: suspend CommunicationScope.(signal: S) -> Unit
): SignalHandler {
	return SignalHandler { incomingSignal ->
		incomingSignal.takeIf { it is S }?.let { signal -> onSignal(signal as S) }
	}
}

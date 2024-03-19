package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

fun interface SignalHandler {
	suspend fun onSignal(signal: Transmission.Signal)
}

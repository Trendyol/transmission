package com.trendyol.transmission.components

import android.util.Log
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.module.TransformerModule
import com.trendyol.transmission.transformer.handler.HandlerScope
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal

/**
 * Example module that logs signals and effects
 */
class LoggingModule(
    private val prefix: String = "",
    private val logAction: (String) -> Unit = { Log.d("Logging", it) }
) : TransformerModule {

    override fun configureHandlers(scope: HandlerScope) {
        // Log all signals
        scope.onSignal<Transmission.Signal> { signal ->
            val signalName = signal::class.simpleName ?: "UnknownSignal"
            logAction("$prefix Received signal: $signalName")
        }

        // Log all effects
        scope.onEffect<Transmission.Effect> { effect ->
            val effectName = effect::class.simpleName ?: "UnknownEffect"
            logAction("$prefix Processed effect: $effectName")
        }
    }

    override fun onError(throwable: Throwable) {
        logAction("$prefix Error: ${throwable.message}")
    }
}

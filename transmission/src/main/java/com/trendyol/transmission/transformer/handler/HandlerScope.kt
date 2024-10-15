package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class HandlerScope internal constructor(val handlerRegistry: HandlerRegistry)

class Handlers internal constructor()

class ExtendedHandlers internal constructor()

fun Transformer.extendHandlers(scope: HandlerScope.() -> Unit): ExtendedHandlers {
    HandlerScope(handlerRegistry).apply(scope)
    return ExtendedHandlers()
}

fun Transformer.createHandlers(scope: HandlerScope.() -> Unit): Handlers {
    this.handlerRegistry.clear()
    HandlerScope(handlerRegistry).apply(scope)
    return Handlers()
}

inline fun <reified T : Transmission.Effect> HandlerScope.onEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.effect<T>(lambda)
}

inline fun <reified T : Transmission.Signal> HandlerScope.onSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.signal<T>(lambda)
}

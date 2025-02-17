package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class HandlerScope internal constructor(val handlerRegistry: HandlerRegistry)

class UpdateHandlerScope internal constructor(val handlerRegistry: HandlerRegistry)

class Handlers internal constructor()

fun Transformer.handlers(scope: HandlerScope.() -> Unit = {}): Handlers {
    this.handlerRegistry.clear()
    HandlerScope(handlerRegistry).apply(scope)
    return Handlers()
}

fun Handlers.update(
    transformer: Transformer,
    scope: UpdateHandlerScope.() -> Unit = {}
): Handlers {
    UpdateHandlerScope(transformer.handlerRegistry).apply(scope)
    return Handlers()
}

inline fun <reified T : Transmission.Effect> UpdateHandlerScope.extendEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.extendEffect<T>(lambda)
}

inline fun <reified T : Transmission.Signal> UpdateHandlerScope.extendSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.extendSignal<T>(lambda)
}

inline fun <reified T : Transmission.Effect> UpdateHandlerScope.overrideEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.effect<T>(lambda)
}

inline fun <reified T : Transmission.Signal> UpdateHandlerScope.overrideSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.signal<T>(lambda)
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

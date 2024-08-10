package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class HandlerScope internal constructor(val handlerRegistry: HandlerRegistry)

fun Transformer.handlers(scope: HandlerScope.() -> Unit): HandlerRegistry {
    val handlerRegistry = HandlerRegistry()
    HandlerScope(handlerRegistry).apply(scope)
    return handlerRegistry
}

inline fun <reified T : Transmission.Effect> HandlerScope.effect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.effect<T>(lambda)
}

inline fun <reified T : Transmission.Signal> HandlerScope.signal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.signal<T>(lambda)
}

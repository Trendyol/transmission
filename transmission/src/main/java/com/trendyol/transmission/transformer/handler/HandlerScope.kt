package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

/**
* Scope for adding handlers to a transformer
*/
class HandlerScope internal constructor(
    val handlerRegistry: HandlerRegistry,
    val sourceClass: KClass<out Transformer>,
)


/**
 * Add a handler for a specific signal type
 */
inline fun <reified T : Transmission.Signal> HandlerScope.onSignal(
    noinline handler: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.addSignalHandler(T::class, sourceClass, handler)
}

/**
 * Add a handler for a specific effect type
 */
inline fun <reified T : Transmission.Effect> HandlerScope.onEffect(
    noinline handler: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.addEffectHandler(T::class, sourceClass, handler)
}

/**
 * Extend a handler for a specific signal type
 */
inline fun <reified T : Transmission.Signal> HandlerScope.extendSignal(
    noinline handler: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.extendSignalHandler(T::class, sourceClass, handler)
}

/**
 * Extend a handler for a specific effect type
 */
inline fun <reified T : Transmission.Effect> HandlerScope.extendEffect(
    noinline handler: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.extendEffectHandler(T::class, sourceClass, handler)
}

class Handlers internal constructor()

fun Transformer.handlers(scope: HandlerScope.() -> Unit = {}): Handlers {
    this.handlerRegistry.clear()
    HandlerScope(handlerRegistry, this::class).apply(scope)
    this.initializeModules()
    return Handlers()
}

internal fun Transformer.addToHandlers(scope: HandlerScope.() -> Unit = {}): Handlers {
    HandlerScope(handlerRegistry, this::class).apply(scope)
    return Handlers()
}



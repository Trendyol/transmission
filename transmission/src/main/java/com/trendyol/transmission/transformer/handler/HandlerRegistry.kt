@file:Suppress("UNCHECKED_CAST")

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

class HandlerScope(val handlerRegistry: HandlerRegistry)

fun Transformer.handlerRegistry(scope: HandlerScope.() -> Unit): HandlerRegistry {
    val handlerRegistry = HandlerRegistry()
    HandlerScope(handlerRegistry).apply(scope)
    return handlerRegistry
}

inline fun <reified T : Transmission.Effect> HandlerScope.registerEffect(noinline lambda: suspend CommunicationScope.(effect: T) -> Unit) {
    handlerRegistry.registerEffect<T>(lambda)
}

inline fun <reified T : Transmission.Signal> HandlerScope.registerSignal(noinline lambda: suspend CommunicationScope.(signal: T) -> Unit) {
    handlerRegistry.registerSignal<T>(lambda)
}

class HandlerRegistry internal constructor() {

    @PublishedApi
    internal val signalHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Signal>, suspend CommunicationScope.(effect: Transmission.Signal) -> Unit>()

    @PublishedApi
    internal val effectHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Effect>, suspend CommunicationScope.(effect: Transmission.Effect) -> Unit>()

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> registerSignal(noinline lambda: suspend CommunicationScope.(signal: T) -> Unit) {
        signalHandlerRegistry[T::class] =
            lambda as suspend CommunicationScope.(Transmission.Signal) -> Unit
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> registerEffect(noinline lambda: suspend CommunicationScope.(effect: T) -> Unit) {
        effectHandlerRegistry[T::class] =
            lambda as suspend CommunicationScope.(Transmission.Effect) -> Unit
    }

}

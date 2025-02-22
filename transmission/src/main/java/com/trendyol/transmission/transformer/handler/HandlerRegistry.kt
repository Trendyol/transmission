@file:Suppress("UNCHECKED_CAST")

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import kotlin.reflect.KClass

class HandlerRegistry internal constructor() {

    internal fun clear() {
        signalHandlerRegistry.clear()
        effectHandlerRegistry.clear()
    }

    @PublishedApi
    internal val signalHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Signal>, suspend CommunicationScope.(effect: Transmission.Signal) -> Unit>()

    @PublishedApi
    internal val effectHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Effect>, suspend CommunicationScope.(effect: Transmission.Effect) -> Unit>()

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> signal(
        noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        signalHandlerRegistry[T::class] =
            lambda as suspend CommunicationScope.(Transmission.Signal) -> Unit
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> effect(
        noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        effectHandlerRegistry[T::class] =
            lambda as suspend CommunicationScope.(Transmission.Effect) -> Unit
    }
}

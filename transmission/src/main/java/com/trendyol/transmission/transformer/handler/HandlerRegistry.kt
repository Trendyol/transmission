@file:Suppress("UNCHECKED_CAST")

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import kotlin.reflect.KClass

typealias SignalLambda = TransmissionLambda<Transmission.Signal>
typealias EffectLambda = TransmissionLambda<Transmission.Effect>

class HandlerRegistry internal constructor() {

    internal fun clear() {
        signalHandlerRegistry.clear()
        effectHandlerRegistry.clear()
    }

    @PublishedApi
    internal val signalHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Signal>, StackedLambda<Transmission.Signal>>()

    @PublishedApi
    internal val effectHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Effect>, StackedLambda<Transmission.Effect>>()

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> signal(
        noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        signalHandlerRegistry[T::class] = StackedLambda<Transmission.Signal>()
            .also { it.addOperation(lambda as SignalLambda) }
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> extendSignal(
        noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        signalHandlerRegistry[T::class] =
            signalHandlerRegistry[T::class]?.also { it.addOperation(lambda as SignalLambda) }
                ?: StackedLambda()
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> effect(
        noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        effectHandlerRegistry[T::class] =
            StackedLambda<Transmission.Effect>().also { it.addOperation(lambda as EffectLambda) }
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> extendEffect(
        noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        effectHandlerRegistry[T::class] =
            effectHandlerRegistry[T::class]?.also { it.addOperation(lambda as EffectLambda) }
                ?: StackedLambda()
    }
}

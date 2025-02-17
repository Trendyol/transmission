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
        mutableMapOf<KClass<out Transmission.Signal>, SignalLambdaStack>()

    @PublishedApi
    internal val effectHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Effect>, EffectLambdaStack>()

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> signal(
        noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        signalHandlerRegistry[T::class] =
            SignalLambdaStack().also { it.addOperation(lambda as SignalLambda) }
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Signal> extendSignal(
        noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        signalHandlerRegistry[T::class] =
            signalHandlerRegistry[T::class]?.also { it.addOperation(lambda as SignalLambda) }
                ?: SignalLambdaStack()
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> effect(
        noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        effectHandlerRegistry[T::class] =
            EffectLambdaStack().also { it.addOperation(lambda as EffectLambda) }
    }

    @PublishedApi
    internal inline fun <reified T : Transmission.Effect> extendEffect(
        noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        effectHandlerRegistry[T::class] =
            effectHandlerRegistry[T::class]?.also { it.addOperation(lambda as EffectLambda) }
                ?: EffectLambdaStack()
    }
}

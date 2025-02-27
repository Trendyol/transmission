package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

class UpdateHandlerScope internal constructor(val handlerRegistry: HandlerRegistry,val source: KClass<out Transformer>)

fun Handlers.update(
    transformer: Transformer,
    scope: UpdateHandlerScope.() -> Unit = {}
): Handlers {
    UpdateHandlerScope(transformer.handlerRegistry, transformer::class).apply(scope)
    return Handlers()
}

inline fun <reified T : Transmission.Effect> UpdateHandlerScope.extendEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.extendEffectHandler(T::class, source, lambda)
}

inline fun <reified T : Transmission.Signal> UpdateHandlerScope.extendSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.extendSignalHandler(T::class, source, lambda)
}

inline fun <reified T : Transmission.Effect> UpdateHandlerScope.overrideEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.addEffectHandler(T::class, source, lambda)
}

inline fun <reified T : Transmission.Signal> UpdateHandlerScope.overrideSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.addSignalHandler(T::class, source, lambda)
}

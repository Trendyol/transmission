package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

typealias SignalLambda = suspend CommunicationScope.(signal: Transmission.Signal) -> Unit
typealias EffectLambda = suspend CommunicationScope.(effect: Transmission.Effect) -> Unit

/**
 * Registry for signal and effect handlers
 */
class HandlerRegistry internal constructor() {

    /**
     * Clear all handlers
     */
    internal fun clear() {
        signalHandlerRegistry.clear()
        effectHandlerRegistry.clear()
    }

    /**
     * Registry mapping signal classes to their handlers
     * Each signal class can have multiple handlers
     * (one per source class that added a handler for it)
     */
    internal val signalHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Signal>, MutableList<IdentifiedHandler<Transmission.Signal>>>()

    /**
     * Registry mapping effect classes to their handlers
     * Each effect class can have multiple handlers
     * (one per source class that added a handler for it)
     */
    internal val effectHandlerRegistry =
        mutableMapOf<KClass<out Transmission.Effect>, MutableList<IdentifiedHandler<Transmission.Effect>>>()

    /**
     * A handler with an identifier to track its source
     */
    internal data class IdentifiedHandler<T>(
        val sourceClass: KClass<*>,
        val handler: StackedLambda<T>
    )

    /**
     * Add a signal handler
     * @param signalClass The class of signal to handle
     * @param sourceClass The class that is adding this handler (used for identification)
     * @param handler The handler function
     */
    @PublishedApi
    internal fun <T : Transmission.Signal> addSignalHandler(
        signalClass: KClass<T>,
        sourceClass: KClass<*>,
        handler: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        // Create a new stacked lambda for this handler
        val stackedLambda = StackedLambda<Transmission.Signal>()
        @Suppress("UNCHECKED_CAST")
        stackedLambda.addOperation(handler as SignalLambda)

        // Create the identified handler
        val identifiedHandler = IdentifiedHandler(sourceClass, stackedLambda)

        // Register the handler for the specified signal class
        signalHandlerRegistry.getOrPut(signalClass) { mutableListOf() }.add(identifiedHandler)

        // Find all registered subclasses of this signal class and register the handler for them too
        // Only do this for supertypes, not for specific types
        for (registeredClass in signalHandlerRegistry.keys.toList()) {
            if (registeredClass != signalClass && registeredClass.isSubclassOf(signalClass)) {
                // For each subclass, add this handler
                val subclassHandlers =
                    signalHandlerRegistry.getOrPut(registeredClass) { mutableListOf() }

                // Create a new stacked lambda with the same operation
                val subclassStackedLambda = StackedLambda<Transmission.Signal>()
                @Suppress("UNCHECKED_CAST")
                subclassStackedLambda.addOperation(handler as SignalLambda)

                // Add as a new identified handler
                subclassHandlers.add(IdentifiedHandler(sourceClass, subclassStackedLambda))
            }
        }
    }

    /**
     * Add an effect handler
     * @param effectClass The class of effect to handle
     * @param sourceClass The class that is adding this handler (used for identification)
     * @param handler The handler function
     */
    @PublishedApi
    internal fun <T : Transmission.Effect> addEffectHandler(
        effectClass: KClass<T>,
        sourceClass: KClass<*>,
        handler: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        // Create a new stacked lambda for this handler
        val stackedLambda = StackedLambda<Transmission.Effect>()
        @Suppress("UNCHECKED_CAST")
        stackedLambda.addOperation(handler as EffectLambda)

        // Create the identified handler
        val identifiedHandler = IdentifiedHandler(sourceClass, stackedLambda)

        // Register the handler for the specified effect class
        effectHandlerRegistry.getOrPut(effectClass) { mutableListOf() }.add(identifiedHandler)

        // Find all registered subclasses of this effect class and register the handler for them too
        // Only do this for supertypes, not for specific types
        for (registeredClass in effectHandlerRegistry.keys.toList()) {
            if (registeredClass != effectClass && registeredClass.isSubclassOf(effectClass)) {
                // For each subclass, add this handler
                val subclassHandlers =
                    effectHandlerRegistry.getOrPut(registeredClass) { mutableListOf() }

                // Create a new stacked lambda with the same operation
                val subclassStackedLambda = StackedLambda<Transmission.Effect>()
                @Suppress("UNCHECKED_CAST")
                subclassStackedLambda.addOperation(handler as EffectLambda)

                // Add as a new identified handler
                subclassHandlers.add(IdentifiedHandler(sourceClass, subclassStackedLambda))
            }
        }
    }

    /**
     * Add a signal handler using reified type parameters
     */
    @PublishedApi
    internal inline fun <reified T : Transmission.Signal, reified S : Any> addSignalHandler(
        noinline handler: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        addSignalHandler(T::class, S::class, handler)
    }

    /**
     * Add an effect handler using reified type parameters
     */
    @PublishedApi
    internal inline fun <reified T : Transmission.Effect, reified S : Any> addEffectHandler(
        noinline handler: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        addEffectHandler(T::class, S::class, handler)
    }

    /**
     * Extend a signal handler for a specific source class
     * @param signalClass The class of signal to handle
     * @param sourceClass The class that originally added the handler to extend
     * @param handler The handler function to add
     */
    @PublishedApi
    internal fun <T : Transmission.Signal> extendSignalHandler(
        signalClass: KClass<T>,
        sourceClass: KClass<*>,
        handler: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        // Find the handlers for this signal class
        val handlers = signalHandlerRegistry[signalClass]

        if (handlers != null) {
            // Find the handler added by the source class
            val sourceHandler = handlers.find { it.sourceClass == sourceClass }

            if (sourceHandler != null) {
                // Add the new operation to the existing handler
                @Suppress("UNCHECKED_CAST")
                sourceHandler.handler.addOperation(handler as SignalLambda)

                // Now extend this for all subclasses too
                for ((subClass, subHandlers) in signalHandlerRegistry) {
                    if (subClass != signalClass && subClass.isSubclassOf(signalClass)) {
                        // Find the handler for the same source class
                        val subSourceHandler = subHandlers.find { it.sourceClass == sourceClass }

                        if (subSourceHandler != null) {
                            // Add the operation to the existing handler
                            @Suppress("UNCHECKED_CAST")
                            subSourceHandler.handler.addOperation(handler as SignalLambda)
                        }
                    }
                }
            } else {
                // No handler from this source class, create a new one
                addSignalHandler(signalClass, sourceClass, handler)
            }
        } else {
            // No handlers for this class at all, create a new one
            addSignalHandler(signalClass, sourceClass, handler)
        }
    }

    /**
     * Extend an effect handler for a specific source class
     * @param effectClass The class of effect to handle
     * @param sourceClass The class that originally added the handler to extend
     * @param handler The handler function to add
     */
    @PublishedApi
    internal fun <T : Transmission.Effect> extendEffectHandler(
        effectClass: KClass<T>,
        sourceClass: KClass<*>,
        handler: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        // Find the handlers for this effect class
        val handlers = effectHandlerRegistry[effectClass]

        if (handlers != null) {
            // Find the handler added by the source class
            val sourceHandler = handlers.find { it.sourceClass == sourceClass }

            if (sourceHandler != null) {
                // Add the new operation to the existing handler
                @Suppress("UNCHECKED_CAST")
                sourceHandler.handler.addOperation(handler as EffectLambda)

                // Now extend this for all subclasses too
                for ((subClass, subHandlers) in effectHandlerRegistry) {
                    if (subClass != effectClass && subClass.isSubclassOf(effectClass)) {
                        // Find the handler for the same source class
                        val subSourceHandler = subHandlers.find { it.sourceClass == sourceClass }

                        if (subSourceHandler != null) {
                            // Add the operation to the existing handler
                            @Suppress("UNCHECKED_CAST")
                            subSourceHandler.handler.addOperation(handler as EffectLambda)
                        }
                    }
                }
            } else {
                // No handler from this source class, create a new one
                addEffectHandler(effectClass, sourceClass, handler)
            }
        } else {
            // No handlers for this class at all, create a new one
            addEffectHandler(effectClass, sourceClass, handler)
        }
    }

    /**
     * Extend a signal handler using reified type parameters
     */
    @PublishedApi
    internal inline fun <reified T : Transmission.Signal, reified S : Any> extendSignalHandler(
        noinline handler: suspend CommunicationScope.(signal: T) -> Unit
    ) {
        extendSignalHandler(T::class, S::class, handler)
    }

    /**
     * Extend an effect handler using reified type parameters
     */
    @PublishedApi
    internal inline fun <reified T : Transmission.Effect, reified S : Any> extendEffectHandler(
        noinline handler: suspend CommunicationScope.(effect: T) -> Unit
    ) {
        extendEffectHandler(T::class, S::class, handler)
    }

    /**
     * Execute all handlers for a specific signal
     */
    internal suspend fun executeSignalHandlers(
        scope: CommunicationScope,
        signal: Transmission.Signal
    ) {
        val signalClass = signal::class

        // Get the handlers for this exact class
        val handlers = signalHandlerRegistry[signalClass]

        // Execute all handlers if present
        handlers?.forEach { identifiedHandler ->
            identifiedHandler.handler.execute(scope, signal)
        }
    }

    /**
     * Execute all handlers for a specific effect
     */
    internal suspend fun executeEffectHandlers(
        scope: CommunicationScope,
        effect: Transmission.Effect
    ) {
        val effectClass = effect::class

        // Get the handlers for this exact class
        val handlers = effectHandlerRegistry[effectClass]

        // Execute all handlers if present
        handlers?.forEach { identifiedHandler ->
            identifiedHandler.handler.execute(scope, effect)
        }
    }
}
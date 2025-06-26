package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

/**
 * DSL scope for defining signal and effect handlers within transformers.
 * 
 * HandlerScope provides a type-safe way to register handlers for specific signal and effect types.
 * Handlers defined within this scope will be invoked when matching signals or effects are processed.
 * 
 * @see onSignal for registering signal handlers
 * @see onEffect for registering effect handlers
 */
class HandlerScope internal constructor(val handlerRegistry: HandlerRegistry)

/**
 * Marker class representing a collection of handlers defined for a transformer.
 * 
 * This class is returned from the handlers DSL to indicate that handlers have been
 * successfully registered with the transformer.
 */
class Handlers internal constructor()

/**
 * Creates and registers signal and effect handlers for this transformer.
 * 
 * This function provides a DSL for defining how the transformer should respond to
 * different types of signals and effects. Each handler is executed within a
 * [CommunicationScope] that provides access to communication methods.
 * 
 * Note: Calling this function clears any previously registered handlers.
 * 
 * @param scope DSL lambda for defining handlers
 * @return A [Handlers] instance representing the registered handlers
 * 
 * Example usage:
 * ```kotlin
 * override val handlers: Handlers = handlers {
 *     onSignal<UserSignal.Login> { signal ->
 *         val user = authenticateUser(signal.credentials)
 *         send(UserData.LoggedIn(user))
 *     }
 *     
 *     onEffect<CacheEffect.Invalidate> { effect ->
 *         cache.clear()
 *         publish(CacheEffect.Cleared)
 *     }
 * }
 * ```
 * 
 * @see HandlerScope for available handler registration methods
 * @see CommunicationScope for available communication methods within handlers
 */
fun Transformer.handlers(scope: HandlerScope.() -> Unit = {}): Handlers {
    this.handlerRegistry.clear()
    HandlerScope(handlerRegistry).apply(scope)
    return Handlers()
}

/**
 * Registers a handler for effects of type [T].
 * 
 * The handler lambda will be invoked whenever an effect of the specified type is processed
 * by the transformer. The handler receives the effect instance and executes within a
 * [CommunicationScope] that provides communication capabilities.
 * 
 * @param T The specific effect type to handle
 * @param lambda Suspend function to execute when an effect of type [T] is received
 * 
 * Example usage:
 * ```kotlin
 * onEffect<NetworkEffect.ConnectionLost> { effect ->
 *     // Handle network disconnection
 *     send(AppData.NetworkStatus(isConnected = false))
 * }
 * 
 * onEffect<UserEffect.ProfileUpdated> { effect ->
 *     // Sync profile changes
 *     execute(syncProfileContract, effect.userId)
 * }
 * ```
 * 
 * @see CommunicationScope for available operations within the handler
 */
inline fun <reified T : Transmission.Effect> HandlerScope.onEffect(
    noinline lambda: suspend CommunicationScope.(effect: T) -> Unit
) {
    handlerRegistry.effect<T>(lambda)
}

/**
 * Registers a handler for signals of type [T].
 * 
 * The handler lambda will be invoked whenever a signal of the specified type is processed
 * by the router and routed to this transformer. Signals typically represent user interactions
 * or external events that need processing.
 * 
 * @param T The specific signal type to handle
 * @param lambda Suspend function to execute when a signal of type [T] is received
 * 
 * Example usage:
 * ```kotlin
 * onSignal<UserSignal.Login> { signal ->
 *     val result = authenticateUser(signal.username, signal.password)
 *     when (result) {
 *         is AuthResult.Success -> send(UserData.LoggedIn(result.user))
 *         is AuthResult.Failure -> publish(ErrorEffect.AuthenticationFailed(result.reason))
 *     }
 * }
 * 
 * onSignal<DataSignal.Refresh> { signal ->
 *     val freshData = dataRepository.refresh()
 *     send(AppData.Updated(freshData))
 * }
 * ```
 * 
 * @see CommunicationScope for available operations within the handler
 */
inline fun <reified T : Transmission.Signal> HandlerScope.onSignal(
    noinline lambda: suspend CommunicationScope.(signal: T) -> Unit
) {
    handlerRegistry.signal<T>(lambda)
}

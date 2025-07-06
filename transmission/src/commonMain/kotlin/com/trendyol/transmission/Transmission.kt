package com.trendyol.transmission

/**
 * Base sealed interface for all transmission types in the reactive architecture framework.
 * 
 * Transmission defines the core communication contract for the reactive architecture,
 * providing three fundamental transmission types that represent different stages
 * of data flow and processing:
 * 
 * - [Signal]: Input events from users or external systems
 * - [Effect]: Side effects and inter-transformer communications  
 * - [Data]: Final processed state representing application data
 * 
 * This type system enables type-safe, reactive data flow through transformers
 * and provides clear separation of concerns between input, processing, and output.
 * 
 * @see Signal for user input and external events
 * @see Effect for side effects and inter-transformer communication
 * @see Data for application state and final processed data
 */
sealed interface Transmission {
    /**
     * Represents user inputs and external events that trigger processing in the system.
     * 
     * Signals are the entry points for data flow in the Transmission framework. They typically
     * represent user interactions (clicks, text input, navigation) or external events
     * (network responses, system notifications, timer events) that need processing.
     * 
     * Key characteristics:
     * - Only signals can be directly processed by [TransmissionRouter.process]
     * - Processed by transformers through signal handlers
     * - Often result in effects being published or data being emitted
     * - Should be immutable data classes representing specific events
     * 
     * Example implementations:
     * ```kotlin
     * sealed interface UserSignal : Transmission.Signal {
     *     data class Login(val username: String, val password: String) : UserSignal
     *     data object Logout : UserSignal
     *     data class UpdateProfile(val profile: UserProfile) : UserSignal
     * }
     * ```
     * 
     * @see TransmissionRouter.process for processing signals
     * @see com.trendyol.transmission.transformer.handler.onSignal for handling signals
     */
    interface Signal : Transmission

    /**
     * Represents side effects and inter-transformer communications within the system.
     * 
     * Effects are created as a result of signal processing or other effects, and represent
     * actions that need to be performed or communications between transformers. They enable
     * loose coupling between transformers while maintaining reactive data flow.
     * 
     * Key characteristics:
     * - Created from signals or other effects during processing
     * - Can be broadcast to all transformers or sent to specific ones
     * - Enable inter-transformer communication and coordination
     * - Represent side effects like network calls, cache operations, or notifications
     * 
     * Example implementations:
     * ```kotlin
     * sealed interface CacheEffect : Transmission.Effect {
     *     data object ClearCache : CacheEffect
     *     data class InvalidateKey(val key: String) : CacheEffect
     * }
     * ```
     * 
     * @see com.trendyol.transmission.transformer.handler.CommunicationScope.publish for broadcasting effects
     * @see com.trendyol.transmission.transformer.handler.onEffect for handling effects
     */
    interface Effect : Transmission

    /**
     * Represents the final processed data that holds the current application state.
     * 
     * Data represents the output of signal and effect processing, typically containing
     * the current state of the application or specific features. Data instances are
     * emitted by transformers and can be observed by UI components or other consumers.
     * 
     * Key characteristics:
     * - Final output of transformer processing
     * - Represents current application state or feature state
     * - Should be immutable snapshots of state
     * - Can be observed through data streams
     * - Often stored in data holders within transformers
     * 
     * Example implementations:
     * ```kotlin
     * sealed interface AppState : Transmission.Data {
     *     data class UserLoggedIn(val user: User) : AppState
     *     data object UserLoggedOut : AppState
     *     data class Loading(val isLoading: Boolean) : AppState
     * }
     * 
     * data class UserListData(
     *     val users: List<User>,
     *     val isLoading: Boolean = false,
     *     val error: String? = null
     * ) : Transmission.Data
     * ```
     * 
     * @see com.trendyol.transmission.transformer.handler.CommunicationScope.send for emitting data
     * @see TransmissionRouter.streamData for observing data
     * @see com.trendyol.transmission.transformer.dataholder.TransmissionDataHolder for managing data
     */
    interface Data : Transmission
}

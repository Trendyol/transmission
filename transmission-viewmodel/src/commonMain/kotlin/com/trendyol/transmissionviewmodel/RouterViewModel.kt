package com.trendyol.transmissionviewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.router.StreamOwner
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.router.streamData
import com.trendyol.transmission.router.streamDataAsState
import com.trendyol.transmission.router.streamEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Configuration options for [RouterViewModel] setup.
 * 
 * @param capacity Buffer capacity for the router's internal streams
 * @param dispatcher Coroutine dispatcher for router operations
 * @param identity Optional identity for the router instance
 */
data class RouterViewModelConfig(
    val capacity: Capacity = Capacity.Default,
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val identity: Contract.Identity = Contract.identity()
)

/**
 * An abstract [ViewModel] that simplifies the integration of [TransmissionRouter] with multiplatform ViewModel architecture.
 * 
 * This class provides a convenient wrapper around [TransmissionRouter] with automatic setup and lifecycle management.
 * It handles the router initialization, stream collection, and cleanup automatically, allowing developers to focus
 * on implementing their business logic through the provided hooks.
 * 
 * ## Key Features:
 * - **Automatic Setup**: Router is configured and initialized automatically
 * - **Lifecycle Management**: Router cleanup is handled in [onCleared]
 * - **Stream Collection**: Data and effect streams are automatically collected in [viewModelScope]
 * - **Hook Methods**: Override [onData], [onEffect], [onProcessSignal], and [onProcessEffect] for custom logic
 * - **Query Support**: Access to [QueryHandler] for querying transformer states
 * - **StateFlow Helpers**: Built-in methods for converting streams to StateFlow
 * 
 * ## Usage Examples:
 * 
 * ### Simple Implementation with Transformer Set:
 * ```kotlin
 * class MyViewModel : RouterViewModel(
 *     setOf(
 *         UserTransformer(),
 *         AuthTransformer(),
 *         DataTransformer()
 *     )
 * ) {
 *     // Create StateFlow from data stream
 *     val userState = streamDataAsState<UserData>(UserData.Empty)
 *     val authState = streamDataAsState<AuthData>(AuthData.LoggedOut)
 *     
 *     override fun onData(data: Transmission.Data) {
 *         when (data) {
 *             is UserData -> updateUserState(data)
 *             is AuthData -> updateAuthState(data)
 *         }
 *     }
 *     
 *     fun login(credentials: Credentials) {
 *         processSignal(AuthSignal.Login(credentials))
 *     }
 * }
 * ```
 * 
 * ### Implementation with Custom Configuration:
 * ```kotlin
 * class FeatureViewModel : RouterViewModel(
 *     loader = MyTransformerSetLoader(),
 *     config = RouterViewModelConfig(
 *         capacity = Capacity.High,
 *         dispatcher = Dispatchers.IO
 *     )
 * ) {
 *     override fun onEffect(effect: Transmission.Effect) {
 *         when (effect) {
 *             is NavigationEffect -> handleNavigation(effect)
 *             is ErrorEffect -> showError(effect)
 *         }
 *     }
 *     
 *     override fun onError(throwable: Throwable) {
 *         // Handle router errors
 *         logError("Router error", throwable)
 *     }
 * }
 * ```
 * 
 * @param loader The [TransformerSetLoader] that provides the transformers for the router
 * @param config Configuration options for the router setup
 */
abstract class RouterViewModel(
    loader: TransformerSetLoader,
    private val config: RouterViewModelConfig = RouterViewModelConfig()
) : ViewModel() {

    /**
     * Convenience constructor that accepts a [Set] of [Transformer]s directly.
     * 
     * This constructor automatically wraps the transformer set in a [TransformerSetLoader]
     * for easier usage when you have a static set of transformers.
     * 
     * @param transformerSet A set of transformers to be used by the router
     * @param config Configuration options for the router setup
     */
    constructor(
        transformerSet: Set<Transformer>,
        config: RouterViewModelConfig = RouterViewModelConfig()
    ) : this(
        loader = object : TransformerSetLoader {
            override suspend fun load(): Set<Transformer> = transformerSet
        },
        config = config
    )

    /**
     * The internal [TransmissionRouter] instance configured with the provided loader.
     * 
     * This router handles all signal processing, data streaming, and effect management.
     * It's automatically initialized and cleaned up with the ViewModel lifecycle.
     */
    private val router = TransmissionRouter(config.identity) {
        addLoader(loader)
        addDispatcher(config.dispatcher)
        setCapacity(config.capacity)
    }

    /**
     * Provides access to the router's streaming capabilities.
     * 
     * Use this to access data and effect streams for custom stream operations:
     * ```kotlin
     * val userDataStream = streamOwner.streamData<UserData>()
     * val loginEffectStream = streamOwner.streamEffect<LoginEffect>()
     * ```
     */
    protected val streamOwner: StreamOwner = router

    /**
     * Provides access to the router's query capabilities for requesting data from transformers.
     * 
     * Use this to query transformer states and perform computations:
     * ```kotlin
     * suspend fun getCurrentUser(): UserData? {
     *     return queryHandler.getData(UserTransformer.dataContract)
     * }
     * ```
     */
    protected val queryHandler: QueryHandler = router.queryHelper

    init {
        // Automatically collect data and effect streams in viewModelScope
        viewModelScope.launch {
            launch {
                try {
                    router.streamData().collect(::onData)
                } catch (throwable: Throwable) {
                    onError(throwable)
                }
            }
            launch {
                try {
                    router.streamEffect().collect(::onEffect)
                } catch (throwable: Throwable) {
                    onError(throwable)
                }
            }
        }
    }

    // region Stream Helpers

    /**
     * Creates a [StateFlow] from a specific data type stream with the provided initial value.
     * 
     * This is a convenience method that combines [streamOwner.streamData] with [StateFlow] conversion.
     * The resulting StateFlow will emit whenever a transformer publishes data of type [T].
     * 
     * @param T The type of data to stream
     * @param initialValue The initial value for the StateFlow
     * @param started When to start and stop sharing the flow (default: WhileSubscribed())
     * @return A StateFlow that emits data of type T
     */
    protected inline fun <reified T : Transmission.Data> streamDataAsState(
        initialValue: T,
        started: SharingStarted = SharingStarted.WhileSubscribed()
    ): StateFlow<T> {
        return streamOwner.streamDataAsState(viewModelScope, initialValue, started)
    }

    /**
     * Creates a [Flow] from a specific data type stream.
     * 
     * This is a convenience method for accessing typed data streams.
     * 
     * @param T The type of data to stream
     * @return A Flow that emits data of type T
     */
    protected inline fun <reified T : Transmission.Data> streamData(): Flow<T> {
        return streamOwner.streamData<T>()
    }

    /**
     * Creates a [Flow] from a specific effect type stream.
     * 
     * This is a convenience method for accessing typed effect streams.
     * 
     * @param T The type of effect to stream
     * @return A Flow that emits effects of type T
     */
    protected inline fun <reified T : Transmission.Effect> streamEffect(): Flow<T> {
        return streamOwner.streamEffect<T>()
    }

    // endregion

    /**
     * Called whenever a [Transmission.Data] is emitted by any transformer in the router.
     * 
     * Override this method to handle data updates from transformers. This is typically where
     * you would update your ViewModel's state based on transformer outputs.
     * 
     * **Note**: This method is called on the [viewModelScope] and is safe for UI updates.
     * 
     * @param data The data emitted by a transformer
     */
    protected open fun onData(data: Transmission.Data): Unit {}

    /**
     * Called whenever a [Transmission.Effect] is emitted by any transformer in the router.
     * 
     * Override this method to handle side effects from transformers. This is typically where
     * you would handle navigation, error display, or other side effects.
     * 
     * **Note**: This method is called on the [viewModelScope] and is safe for UI updates.
     * 
     * @param effect The effect emitted by a transformer
     */
    protected open fun onEffect(effect: Transmission.Effect): Unit {}

    /**
     * Called when an error occurs during stream collection.
     * 
     * Override this method to handle errors from the router or transformers.
     * This could include network errors, transformation errors, or other exceptions.
     * 
     * @param throwable The error that occurred
     */
    protected open fun onError(throwable: Throwable): Unit {}

    /**
     * Processes a [Transmission.Signal] through the router.
     * 
     * This is the primary method for sending user interactions and UI events to the transformers.
     * After processing the signal through the router, it calls [onProcessSignal] for any
     * additional custom logic.
     * 
     * ## Example Usage:
     * ```kotlin
     * // In your ViewModel
     * fun onUserClickLogin(credentials: Credentials) {
     *     processSignal(AuthSignal.Login(credentials))
     * }
     * 
     * // In your UI
     * Button(onClick = { viewModel.onUserClickLogin(credentials) }) {
     *     Text("Login")
     * }
     * ```
     * 
     * @param signal The signal to process
     */
    fun processSignal(signal: Transmission.Signal) {
        try {
            router.process(signal)
            onProcessSignal(signal)
        } catch (throwable: Throwable) {
            onError(throwable)
        }
    }

    /**
     * Called after a signal has been processed by the router.
     * 
     * Override this method to implement custom logic that should execute after signal processing,
     * such as logging, analytics, or additional side effects.
     * 
     * **Note**: This method is called on the same thread as [processSignal].
     * 
     * @param signal The signal that was processed
     */
    protected open fun onProcessSignal(signal: Transmission.Signal): Unit {}

    /**
     * Processes a [Transmission.Effect] through the router.
     * 
     * This method allows you to manually send effects to the router, which can be useful for
     * triggering cross-transformer communication or sending custom effects from the ViewModel layer.
     * After processing the effect through the router, it calls [onProcessEffect] for any
     * additional custom logic.
     * 
     * ## Example Usage:
     * ```kotlin
     * // Trigger a manual refresh
     * fun refreshData() {
     *     processEffect(RefreshDataEffect())
     * }
     * 
     * // Send a custom logging effect
     * fun logUserAction(action: String) {
     *     processEffect(LoggingEffect("User action: $action"))
     * }
     * ```
     * 
     * @param effect The effect to process
     */
    fun processEffect(effect: Transmission.Effect) {
        try {
            router.process(effect)
            onProcessEffect(effect)
        } catch (throwable: Throwable) {
            onError(throwable)
        }
    }

    /**
     * Called after an effect has been processed by the router.
     * 
     * Override this method to implement custom logic that should execute after effect processing,
     * such as logging, analytics, or additional side effects.
     * 
     * **Note**: This method is called on the same thread as [processEffect].
     * 
     * @param effect The effect that was processed
     */
    protected open fun onProcessEffect(effect: Transmission.Effect): Unit {}

    /**
     * Automatically cleans up the router when the ViewModel is cleared.
     * 
     * This ensures proper cleanup of all transformer resources, coroutines, and prevents memory leaks.
     * The router's [TransmissionRouter.clear] method is called, which:
     * - Cancels all running coroutines
     * - Clears all transformer states
     * - Releases all resources
     * 
     * **Note**: You typically don't need to override this method unless you have additional cleanup logic.
     * If you do override it, make sure to call `super.onCleared()`.
     */
    override fun onCleared() {
        router.clear()
        super.onCleared()
    }
}
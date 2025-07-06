package com.trendyol.transmission.router

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffectWithType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlin.jvm.JvmName

/**
 * Creates a flow of all [Transmission.Data] emitted by transformers.
 * 
 * This extension function provides access to the complete data stream from a [StreamOwner],
 * typically a [TransmissionRouter]. Use this to observe all data emissions regardless of type.
 * 
 * @return Flow of all data emitted by transformers
 * 
 * Example usage:
 * ```kotlin
 * router.streamData().collect { data ->
 *     when (data) {
 *         is UserData -> handleUserData(data)
 *         is ProductData -> handleProductData(data)
 *         else -> handleUnknownData(data)
 *     }
 * }
 * ```
 * 
 * @see streamData<T> for type-filtered data streaming
 */
@JvmName("streamData")
fun StreamOwner.streamData(): Flow<Transmission.Data> {
    return this.dataStream
}

/**
 * Creates a flow of [Transmission.Data] filtered by the specified type [T].
 * 
 * This extension function provides type-safe access to specific data types from a [StreamOwner].
 * Only data instances that match the reified type [T] will be emitted.
 * 
 * @param T The specific data type to filter for
 * @return Flow containing only data of type [T]
 * 
 * Example usage:
 * ```kotlin
 * router.streamData<UserData>().collect { userData ->
 *     updateUserUI(userData.user)
 * }
 * 
 * router.streamData<ProductData>().collect { productData ->
 *     updateProductList(productData.products)
 * }
 * ```
 * 
 * @see streamData() for unfiltered data streaming
 * @see streamDataWithAction for combining filtering with side effects
 */
@JvmName("streamDataWithType")
inline fun <reified T : Transmission.Data> StreamOwner.streamData(): Flow<T> {
    return this.dataStream.filterIsInstance<T>()
}

/**
 * Creates a flow of [Transmission.Data] filtered by type [T] with a side effect action.
 * 
 * This extension function combines type filtering with a side effect action that is
 * executed for each data emission. Useful for logging, analytics, or other side effects
 * while maintaining the reactive stream.
 * 
 * @param T The specific data type to filter for
 * @param action Suspend function to execute for each data emission
 * @return Flow containing only data of type [T], with the action executed for each emission
 * 
 * Example usage:
 * ```kotlin
 * router.streamData<UserData> { userData ->
 *     analytics.track("UserDataUpdated", userData.user.id)
 * }.collect { userData ->
 *     updateUserUI(userData.user)
 * }
 * ```
 * 
 * @see streamData<T> for type-filtered streaming without side effects
 */
@JvmName("streamDataWithAction")
inline fun <reified T : Transmission.Data> StreamOwner.streamData(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.dataStream.filterIsInstance<T>().onEach(action)
}

/**
 * Creates a flow of [Transmission.Effect] filtered by the specified type [T].
 * 
 * This extension function provides type-safe access to specific effect types from a [StreamOwner].
 * Only effects that match the reified type [T] will be emitted. Useful for observing
 * specific effects produced by transformers.
 * 
 * @param T The specific effect type to filter for
 * @return Flow containing only effects of type [T]
 * 
 * Example usage:
 * ```kotlin
 * router.streamEffect<NavigationEffect>().collect { navigationEffect ->
 *     when (navigationEffect) {
 *         is NavigationEffect.GoToLogin -> navigateToLogin()
 *         is NavigationEffect.GoBack -> navigateBack()
 *     }
 * }
 * ```
 * 
 * @see streamEffectWithAction for combining filtering with side effects
 */
@JvmName("streamEffect")
inline fun <reified T : Transmission.Effect> StreamOwner.streamEffect(): Flow<T> {
    return this.effectStream.filterIsInstance<T>()
}

/**
 * Creates a flow of all [Transmission.Effect] emitted by transformers.
 * 
 * This extension function provides access to the complete effect stream from a [TransmissionRouter].
 * Use this to observe all effect emissions regardless of type.
 * 
 * @return Flow of all effects emitted by transformers
 * 
 * Example usage:
 * ```kotlin
 * router.streamEffect().collect { effect ->
 *     when (effect) {
 *         is NavigationEffect -> handleNavigation(effect)
 *         is NetworkEffect -> handleNetworkEvent(effect)
 *         else -> handleUnknownEffect(effect)
 *     }
 * }
 * ```
 * 
 * @see streamEffect<T> for type-filtered effect streaming
 */
@JvmName("streamEffectWithType")
fun TransmissionRouter.streamEffect(): Flow<Transmission.Effect> {
    return this.effectStream
}

/**
 * Creates a flow of [Transmission.Effect] filtered by type [T] with a side effect action.
 * 
 * This extension function combines type filtering with a side effect action that is
 * executed for each effect emission. Useful for logging, analytics, or other side effects
 * while maintaining the reactive stream.
 * 
 * @param T The specific effect type to filter for
 * @param action Suspend function to execute for each effect emission
 * @return Flow containing only effects of type [T], with the action executed for each emission
 * 
 * Example usage:
 * ```kotlin
 * router.streamEffect<ErrorEffect> { errorEffect ->
 *     crashReporter.log("Effect error", errorEffect.throwable)
 * }.collect { errorEffect ->
 *     showErrorDialog(errorEffect.message)
 * }
 * ```
 * 
 * @see streamEffect<T> for type-filtered streaming without side effects
 */
@JvmName("streamEffectWithAction")
inline fun <reified T : Transmission.Effect> StreamOwner.streamEffect(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.effectStream.filterIsInstance<T>().onEach(action)
}

/**
 * Creates a [StateFlow] of [Transmission.Data] filtered by type [T].
 * 
 * This extension function converts a data stream into a state holder that retains the latest
 * value and provides it to new subscribers. Useful for UI components that need to access
 * the current state value rather than just observing changes.
 * 
 * @param T The specific data type to filter and hold in state
 * @param scope CoroutineScope for the StateFlow lifecycle
 * @param initialValue Initial value to emit before any data arrives
 * @param sharingStarted Sharing policy for the StateFlow
 * @return StateFlow containing the latest data of type [T]
 * 
 * Example usage:
 * ```kotlin
 * class MyViewModel : ViewModel() {
 *     val userState: StateFlow<UserData> = router.streamDataAsState(
 *         scope = viewModelScope,
 *         initialValue = UserData.Empty
 *     )
 * }
 * 
 * @Composable
 * fun UserScreen(viewModel: MyViewModel) {
 *     val userData by viewModel.userState.collectAsState()
 *     // Use userData in UI
 * }
 * ```
 * 
 * @see streamData<T> for flow-based data streaming
 * @see asState for converting existing flows to state
 */
inline fun <reified T : Transmission.Data> StreamOwner.streamDataAsState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): StateFlow<T> {
    return this.dataStream.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

/**
 * Converts a [Flow] of [Transmission.Data] into a [StateFlow].
 * 
 * This extension function transforms any data flow into a state holder that retains the
 * latest value. Useful for creating stateful streams from existing data flows.
 * 
 * @param T The data type contained in the flow
 * @param scope CoroutineScope for the StateFlow lifecycle
 * @param initialValue Initial value to emit before any flow data arrives
 * @param sharingStarted Sharing policy for the StateFlow
 * @return StateFlow containing the latest data from the flow
 * 
 * Example usage:
 * ```kotlin
 * val filteredUserState = router
 *     .streamData<UserData>()
 *     .filter { it.isActive }
 *     .asState(
 *         scope = viewModelScope,
 *         initialValue = UserData.Empty
 *     )
 * ```
 * 
 * @see streamDataAsState for direct StateFlow creation from StreamOwner
 */
inline fun <reified T : Transmission.Data> Flow<T>.asState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): StateFlow<T> {
    return this.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

/**
 * Creates a flow of one-shot payload data from [RouterEffectWithType] effects.
 * 
 * This experimental API provides access to arbitrary payload data sent through the router
 * using the `sendPayload` function. The payload is extracted from [RouterEffectWithType]
 * effects and emitted as a typed flow.
 * 
 * ⚠️ **Experimental API**: This API is experimental and may change or be removed in future versions.
 * 
 * @param D The type of payload data to extract and emit
 * @return Flow of payload data of type [D]
 * 
 * Example usage:
 * ```kotlin
 * // In a transformer
 * sendPayload(ErrorInfo("Network error", 500))
 * 
 * // Observing payloads
 * router.oneShotPayloadStream<ErrorInfo>().collect { errorInfo ->
 *     showErrorDialog(errorInfo.message, errorInfo.code)
 * }
 * ```
 * 
 * @see com.trendyol.transmission.transformer.handler.CommunicationScope.sendPayload
 * @see RouterEffectWithType
 */
@ExperimentalTransmissionApi
inline fun <reified D : Any> StreamOwner.oneShotPayloadStream(): Flow<D> {
    return this.effectStream.filterIsInstance<RouterEffectWithType<D>>().map { it.payload }
}

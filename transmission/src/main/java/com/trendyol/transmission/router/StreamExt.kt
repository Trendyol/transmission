package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

fun TransmissionRouter.streamData(): Flow<Transmission.Data> {
    return this.dataStream
}

inline fun <reified T : Transmission.Data> TransmissionRouter.streamData(): Flow<T> {
    return this.dataStream.filterIsInstance<T>()
}

inline fun <reified T : Transmission.Data> TransmissionRouter.streamData(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.dataStream.filterIsInstance<T>().onEach(action)
}

inline fun <reified T : Transmission.Effect> TransmissionRouter.streamEffect(): Flow<T> {
    return this.effectStream.filterIsInstance<T>()
}

fun TransmissionRouter.streamEffect(): Flow<Transmission.Effect> {
    return this.effectStream
}

inline fun <reified T : Transmission.Effect> TransmissionRouter.streamEffect(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.effectStream.filterIsInstance<T>().onEach(action)
}

inline fun <reified T : Transmission.Data> TransmissionRouter.streamDataAsState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): StateFlow<T> {
    return this.dataStream.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

inline fun <reified T : Transmission.Data> Flow<T>.asState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): Flow<T> {
    return this.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

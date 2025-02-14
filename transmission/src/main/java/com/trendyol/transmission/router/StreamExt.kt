package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.WrappedEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@JvmName("streamData")
fun TransmissionRouter.streamData(): Flow<Transmission.Data> {
    return this.dataStream
}

@JvmName("streamDataWithType")
inline fun <reified T : Transmission.Data> TransmissionRouter.streamData(): Flow<T> {
    return this.dataStream.filterIsInstance<T>()
}

@JvmName("streamDataWithAction")
inline fun <reified T : Transmission.Data> TransmissionRouter.streamData(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.dataStream.filterIsInstance<T>().onEach(action)
}

@JvmName("streamEffect")
inline fun <reified T : Transmission.Effect> TransmissionRouter.streamEffect(): Flow<T> {
    return this.effectStream.map { it.effect }.filterIsInstance<T>()
}

@JvmName("streamEffectWithType")
fun TransmissionRouter.streamEffect(): Flow<Transmission.Effect> {
    return this.effectStream.map { it.effect }
}

fun TransmissionRouter.streamWrappedEffect(): Flow<WrappedEffect> {
    return this.effectStream
}

@JvmName("streamEffectWithAction")
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
): StateFlow<T> {
    return this.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

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

@JvmName("streamData")
fun StreamOwner.streamData(): Flow<Transmission.Data> {
    return this.dataStream
}

@JvmName("streamDataWithType")
inline fun <reified T : Transmission.Data> StreamOwner.streamData(): Flow<T> {
    return this.dataStream.filterIsInstance<T>()
}

@JvmName("streamDataWithAction")
inline fun <reified T : Transmission.Data> StreamOwner.streamData(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.dataStream.filterIsInstance<T>().onEach(action)
}

@JvmName("streamEffect")
inline fun <reified T : Transmission.Effect> StreamOwner.streamEffect(): Flow<T> {
    return this.effectStream.filterIsInstance<T>()
}

@JvmName("streamEffectWithType")
fun TransmissionRouter.streamEffect(): Flow<Transmission.Effect> {
    return this.effectStream
}

@JvmName("streamEffectWithAction")
inline fun <reified T : Transmission.Effect> StreamOwner.streamEffect(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.effectStream.filterIsInstance<T>().onEach(action)
}

inline fun <reified T : Transmission.Data> StreamOwner.streamDataAsState(
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

@ExperimentalTransmissionApi
inline fun <reified D : Any> StreamOwner.oneShotPayloadStream(): Flow<D> {
    return this.effectStream.filterIsInstance<RouterEffectWithType<D>>().map { it.payload }
}

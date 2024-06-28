package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

inline fun <reified T : Transmission.Data?> SharedFlow<Transmission.Data?>.toState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): StateFlow<T> {
    return this.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

inline fun <reified T : Transmission.Data?> Flow<Transmission.Data?>.toState(
    scope: CoroutineScope,
    initialValue: T,
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
): StateFlow<T> {
    return this.filterIsInstance<T>().stateIn(scope, sharingStarted, initialValue)
}

inline fun <reified T : Transmission.Data?> Flow<Transmission.Data?>.onEach(
    noinline action: suspend (T) -> Unit
): Flow<T> {
    return this.filterIsInstance<T>().onEach(action)
}

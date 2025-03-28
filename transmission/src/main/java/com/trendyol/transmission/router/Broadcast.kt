package com.trendyol.transmission.router

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn


interface Broadcast<T> {
    val producer: SendChannel<T>
    val output: SharedFlow<T>
}

internal fun <T> CoroutineScope.createBroadcast(
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(),
    capacity: Capacity = Capacity.Default
): Broadcast<T> = object : Broadcast<T> {

    private val _source = Channel<T>(capacity = capacity.value)
    override val producer: SendChannel<T> = _source

    override val output by lazy {
        _source.receiveAsFlow()
            .shareIn(this@createBroadcast, sharingStarted)
    }
}
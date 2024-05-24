package com.trendyol.transmission.router

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

internal class TransmissionCarrier<T>(scope: CoroutineScope) {

    private val _incoming = Channel<T>(capacity = Channel.BUFFERED)

    val incoming: SendChannel<T> = _incoming

    val outGoing = _incoming.receiveAsFlow().shareIn(scope, SharingStarted.Lazily)
}

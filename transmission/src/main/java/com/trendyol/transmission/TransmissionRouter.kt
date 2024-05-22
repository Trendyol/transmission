package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.router.QueryDelegate
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter<D : Transmission.Data, E : Transmission.Effect>(
    internal val transformerSet: Set<Transformer<D, E>>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val queryDelegate: QueryDelegate<D,E> = QueryDelegate(dispatcher)
) : QuerySender<D, E> by queryDelegate {

    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher)

    internal val routerName: String = this::class.java.simpleName

    // region signals

    private val _incomingSignals = Channel<Transmission.Signal>(capacity = Channel.UNLIMITED)

    private val sharedIncomingSignals = _incomingSignals.receiveAsFlow()
        .shareIn(routerScope, SharingStarted.Lazily)

    fun processSignal(signal: Transmission.Signal) {
        _incomingSignals.trySend(signal)
    }

    // endregion

    // region effects

    private val effectChannel =
        Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.UNLIMITED)

    private val sharedIncomingEffects = effectChannel.receiveAsFlow()
        .shareIn(routerScope, SharingStarted.Lazily)

    val effectStream: Flow<E> = sharedIncomingEffects.map { it.effect }

    // endregion

    private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

    val dataStream = outGoingDataChannel.consumeAsFlow()
        .shareIn(routerScope, SharingStarted.Lazily)

    init {
        initialize()
    }

    private fun initialize() {
        queryDelegate.registerRouter(this)
        require(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        routerScope.launch {
            transformerSet.map { transformer ->
                launch {
                    transformer.initialize(
                        incomingSignal = sharedIncomingSignals,
                        incomingEffect = sharedIncomingEffects,
                        outGoingData = outGoingDataChannel,
                        outGoingEffect = effectChannel,
                        outGoingQuery = queryDelegate.outGoingQueryChannel,
                        incomingQueryResponse = queryDelegate.incomingQueryResponse
                    )
                }
            }
        }
    }

    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
        queryDelegate.clear()
    }
}

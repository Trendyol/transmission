package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.router.QueryDelegate
import com.trendyol.transmission.router.TransmissionCarrier
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter<D : Transmission.Data, E : Transmission.Effect>(
    internal val transformerSet: Set<Transformer<D, E>>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher)

    internal val routerName: String = this::class.java.simpleName

    private val signalCarrier = TransmissionCarrier<Transmission.Signal>(routerScope)
    private val effectCarrier =
        TransmissionCarrier<EffectWrapper<E, D, Transformer<D, E>>>(routerScope)
    private val dataCarrier = TransmissionCarrier<D>(routerScope)

    val effectStream: Flow<E> = effectCarrier.outGoing.map { it.effect }
    val dataStream = dataCarrier.outGoing

    private val _queryDelegate = QueryDelegate(dispatcher, this@TransmissionRouter)
    val queryHelper: QuerySender<D, E> = _queryDelegate

    init {
        initialize()
    }

    fun processSignal(signal: Transmission.Signal) {
        signalCarrier.incoming.trySend(signal)
    }

    private fun initialize() {
        require(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        routerScope.launch {
            transformerSet.map { transformer ->
                launch {
                    transformer.initialize(
                        incomingSignal = signalCarrier.outGoing,
                        incomingEffect = effectCarrier.outGoing,
                        outGoingData = dataCarrier.incoming,
                        outGoingEffect = effectCarrier.incoming,
                        outGoingQuery = _queryDelegate.outGoingQuery,
                        incomingQueryResponse = _queryDelegate.incomingQueryResponse
                    )
                }
            }
        }
    }

    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
        _queryDelegate.clear()
    }
}

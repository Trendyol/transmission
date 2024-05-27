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


/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter(
    internal val transformerSet: Set<Transformer>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher)

    internal val routerName: String = this::class.java.simpleName

    private val signalCarrier = TransmissionCarrier<Transmission.Signal>(routerScope)
    private val effectCarrier = TransmissionCarrier<EffectWrapper>(routerScope)
    private val dataCarrier = TransmissionCarrier<Transmission.Data>(routerScope)

    val effectStream: Flow<Transmission.Effect> = effectCarrier.outGoing.map { it.effect }
    val dataStream = dataCarrier.outGoing

    private val _queryDelegate = QueryDelegate(dispatcher, this@TransmissionRouter)
    val queryHelper: QuerySender = _queryDelegate

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

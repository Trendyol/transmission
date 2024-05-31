package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.router.QueryDelegate
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter(
    internal val transformerSet: Set<Transformer>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher)

    internal val routerName: String = this.identifier()

    private val signalBroadcast = routerScope.createBroadcast<Transmission.Signal>()
    private val dataBroadcast = routerScope.createBroadcast<Transmission.Data>()
    private val effectBroadcast = routerScope.createBroadcast<EffectWrapper>()

    val dataStream = dataBroadcast.output
    val effectStream: SharedFlow<Transmission.Effect> = effectBroadcast.output.map { it.effect }
        .shareIn(routerScope, SharingStarted.Lazily)

    private val _queryDelegate = QueryDelegate(routerScope, this@TransmissionRouter)
    val queryHelper: QuerySender = _queryDelegate

    init {
        initialize()
    }

    fun processSignal(signal: Transmission.Signal) {
        signalBroadcast.producer.trySend(signal)
    }

    private fun initialize() {
        require(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        routerScope.launch {
            transformerSet.map { transformer ->
                coroutineScope {
                    transformer.run {
                        launch {
                            startSignalCollection(incoming = signalBroadcast.output)
                        }
                        launch {
                            startDataPublishing(data = dataBroadcast.producer)
                        }
                        launch {
                            startEffectProcessing(
                                producer = effectBroadcast.producer,
                                incoming = effectBroadcast.output
                            )
                        }
                        launch {
                            startQueryProcessing(queryDelegate = _queryDelegate)
                        }
                    }
                }
            }
        }
    }

    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
    }
}

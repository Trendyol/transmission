package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.router.RegistryScopeImpl
import com.trendyol.transmission.router.RequestDelegate
import com.trendyol.transmission.router.createBroadcast
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.RequestHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * Throws [IllegalArgumentException] when supplied [Transformer] set is empty
 */
class TransmissionRouter internal constructor(
    internal val transformerSet: Set<Transformer>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    registryScope: RegistryScopeImpl? = null
) {

    private val routerScope = CoroutineScope(SupervisorJob() + dispatcher)

    internal val routerName: String = this::class.simpleName.orEmpty()

    private val signalBroadcast = routerScope.createBroadcast<Transmission.Signal>()
    private val dataBroadcast = routerScope.createBroadcast<Transmission.Data>()
    private val effectBroadcast = routerScope.createBroadcast<EffectWrapper>()

    val dataStream = dataBroadcast.output
    val effectStream: SharedFlow<Transmission.Effect> = effectBroadcast.output.map { it.effect }
        .shareIn(routerScope, SharingStarted.Lazily)

    private val _requestDelegate = RequestDelegate(
        queryScope = routerScope,
        routerRef = this@TransmissionRouter,
        registry = registryScope
    )
    val requestHelper: RequestHandler = _requestDelegate

    init {
        initialize()
    }

    fun processSignal(signal: Transmission.Signal) {
        signalBroadcast.producer.trySend(signal)
    }

    fun processEffect(effect: Transmission.Effect) {
        effectBroadcast.producer.trySend(EffectWrapper(effect))
    }

    private fun initialize() {
        require(transformerSet.isNotEmpty()) {
            "transformerSet should not be empty"
        }
        routerScope.launch {
            transformerSet.forEach { transformer ->
                transformer.run {
                    startSignalCollection(incoming = signalBroadcast.output)
                    startDataPublishing(data = dataBroadcast.producer)
                    startEffectProcessing(
                        producer = effectBroadcast.producer,
                        incoming = effectBroadcast.output
                    )
                    startQueryProcessing(
                        incomingQuery = _requestDelegate.incomingQueryResponse,
                        outGoingQuery = _requestDelegate.outGoingQuery
                    )
                }
            }
        }
    }

    fun clear() {
        transformerSet.forEach { it.clear() }
        routerScope.cancel()
    }
}

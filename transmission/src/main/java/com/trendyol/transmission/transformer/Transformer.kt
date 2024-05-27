package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.query.ComputationDelegate
import com.trendyol.transmission.transformer.query.Query
import com.trendyol.transmission.transformer.query.QueryResponse
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

typealias DefaultTransformer = Transformer<Transmission.Data, Transmission.Effect>

open class Transformer<D : Transmission.Data, E : Transmission.Effect>(
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    val transformerName: String = this::class.java.simpleName

    protected val transformerScope = CoroutineScope(SupervisorJob() + dispatcher)

    val storage = TransformerStorage<D, E>()

    private val dataChannel: Channel<D> = Channel(capacity = Channel.UNLIMITED)
    private val effectChannel: Channel<EffectWrapper<E, D, Transformer<D, E>>> =
        Channel(capacity = Channel.UNLIMITED)

    private val outGoingQuery: Channel<Query> = Channel(capacity = Channel.BUFFERED)
    private val incomingQueryResponse: Channel<QueryResponse<D>> =
        Channel(capacity = Channel.BUFFERED)

    private val queryResponseSharedFlow = incomingQueryResponse.receiveAsFlow()
        .shareIn(transformerScope, SharingStarted.WhileSubscribed())

    open val signalHandler: SignalHandler<D, E>? = null

    open val effectHandler: EffectHandler<D, E>? = null


    @Suppress("UNCHECKED_CAST")
    val communicationScope: CommunicationScope<D, E> = object : CommunicationScope<D, E> {
        override fun send(data: D?) {
            data?.let { dataChannel.trySend(it) }
        }

        override fun publish(effect: E) {
            effectChannel.trySend(EffectWrapper(effect))
        }

        override fun send(effect: E, to: KClass<out Transformer<D, E>>) {
            effectChannel.trySend(EffectWrapper(effect, to))
        }

        override suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D? {
            outGoingQuery.trySend(
                Query.Data(
                    sender = transformerName,
                    type = type.simpleName.orEmpty()
                )
            )
            return queryResponseSharedFlow
                .filterIsInstance<QueryResponse.Data<D>>()
                .filter { it.type == type.simpleName }
                .first().data
        }

        override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryData(
            type: KClass<D>,
            owner: KClass<out T>
        ): D? {
            outGoingQuery.trySend(
                Query.Data(
                    sender = transformerName,
                    dataOwner = owner.simpleName.orEmpty(),
                    type = type.simpleName.orEmpty()
                )
            )
            return queryResponseSharedFlow
                .filterIsInstance<QueryResponse.Data<D>>()
                .filter { it.type == type.simpleName }
                .first().data
        }

        override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryComputation(
            type: KClass<D>,
            owner: KClass<out T>,
            invalidate: Boolean
        ): D? {
            outGoingQuery.trySend(
                Query.Computation(
                    sender = transformerName,
                    computationOwner = owner.simpleName.orEmpty(),
                    type = type.simpleName.orEmpty(),
                    invalidate = invalidate
                )
            )
            return queryResponseSharedFlow
                .filterIsInstance<QueryResponse.Computation<D>>()
                .filter { it.type == type.simpleName }
                .first().data
        }

    }

    fun initialize(
        incomingSignal: SharedFlow<Transmission.Signal>,
        incomingEffect: SharedFlow<EffectWrapper<E, D, Transformer<D, E>>>,
        incomingQueryResponse: SharedFlow<QueryResponse<D>>,
        outGoingData: SendChannel<D>,
        outGoingEffect: SendChannel<EffectWrapper<E, D, Transformer<D, E>>>,
        outGoingQuery: SendChannel<Query>,
    ) {
        transformerScope.launch {
            launch {
                incomingSignal.collect {
                    transformerScope.launch {
                        signalHandler?.apply {
                            with(communicationScope) {
                                onSignal(it)
                            }
                        }
                    }
                }
            }
            launch {
                incomingEffect.filterNot { it.effect is RouterPayloadEffect }
                    .collect { incomingEffect ->
                        val effectToProcess = incomingEffect.takeIf {
                            incomingEffect.receiver == null
                                    || incomingEffect.receiver == this@Transformer::class
                        }?.effect ?: return@collect
                        transformerScope.launch {
                            effectHandler?.apply {
                                with(communicationScope) {
                                    onEffect(effectToProcess)
                                }
                            }
                        }
                    }
            }
            launch {
                incomingQueryResponse.filter { it.owner == transformerName }.collect {
                    this@Transformer.incomingQueryResponse.trySend(it)
                }
            }
            launch {
                this@Transformer.outGoingQuery.receiveAsFlow().collect { outGoingQuery.trySend(it) }
            }
            launch { dataChannel.receiveAsFlow().collect { outGoingData.trySend(it) } }
            launch {
                effectChannel.receiveAsFlow().collect { outGoingEffect.trySend(it) }
            }
        }
    }

    fun clear() {
        transformerScope.cancel()
        storage.clear()
    }

    // region DataHolder

    inner class TransmissionDataHolder<T : D?>(
        initialValue: T,
        publishUpdates: Boolean,
        identifier: String
    ) {

        private val holder = MutableStateFlow(initialValue)

        val value: T
            get() = holder.value

        init {
            storage.updateHolderDataReferenceToTrack(identifier)
            transformerScope.launch {
                holder.collect {
                    it?.let { holderData ->
                        storage.updateHolderData(holderData)
                        if (publishUpdates) {
                            dataChannel.trySend(it)
                        }
                    }
                }
            }
        }

        fun update(updater: (T) -> @UnsafeVariance T) {
            holder.update(updater)
        }
    }

    /**
     * Throws [IllegalArgumentException] when multiple data holders with same type
     * is defined inside a [Transformer]
     * @param initialValue Initial value of the Data Holder.
     * Must be a type extended from [Transmission.Data]
     * @param [publishUpdates] Controls sending updates to the [TransmissionRouter]
     * */
    inline fun <reified T : D?> Transformer<D, E>.buildDataHolder(
        initialValue: T,
        publishUpdates: Boolean = true,
    ): TransmissionDataHolder<T> {
        return TransmissionDataHolder(initialValue, publishUpdates, T::class.java.simpleName)
    }

    // endregion

    /**
     * Throws [IllegalArgumentException] when multiple computations with the same return type
     * are defined inside the [Transformer].
     *
     * Adds a computation to [Transformer] to be queried.
     * @param useCache Stores the result after first computation
     * @param computation Computation to get the result [Transmission.Data]
     */
    protected inline fun <reified T : D> registerComputation(
        useCache: Boolean = false,
        noinline computation: suspend QuerySender<D, E>.() -> T?,
    ) {
        val typeName = T::class.java.simpleName
        storage.registerComputation(typeName, ComputationDelegate(useCache, computation))
    }

    // region handler extensions

    inline fun <reified S : Transmission.Signal> Transformer<D, E>.buildTypedSignalHandler(
        crossinline onSignal: suspend CommunicationScope<D, E>.(signal: S) -> Unit
    ): SignalHandler<D, E> {
        return SignalHandler { incomingSignal ->
            incomingSignal.takeIf { it is S }?.let { signal -> onSignal(signal as S) }
        }
    }

    inline fun <reified HE : Transmission.Effect> Transformer<D, E>.buildTypedEffectHandler(
        crossinline onEffect: suspend CommunicationScope<D, E>.(effect: HE) -> Unit
    ): EffectHandler<D, E> {
        return EffectHandler { incomingEffect ->
            incomingEffect.takeIf { it is HE }?.let { effect -> onEffect(effect as HE) }
        }
    }

    // endregion

}

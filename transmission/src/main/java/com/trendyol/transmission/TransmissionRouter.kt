package com.trendyol.transmission

import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.HolderState
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.DataQuery
import com.trendyol.transmission.transformer.query.QueryResponse
import com.trendyol.transmission.transformer.query.QuerySender
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

class TransmissionRouter<D : Transmission.Data, E : Transmission.Effect>(
	private val transformerSet: Set<Transformer<D, E>>,
	private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : QuerySender<D, E> {

	private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

	private val routerName: String = this::class.java.simpleName

	private var initializationJob: Job? = null

	// region signals

	private val _incomingSignals = Channel<Transmission.Signal>(capacity = Channel.UNLIMITED)

	private val sharedIncomingSignals = _incomingSignals.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	fun processSignal(signal: Transmission.Signal) {
		_incomingSignals.trySend(signal)
	}

	// endregion

	// region effects

	private val effectChannel =
		Channel<EffectWrapper<E, D, Transformer<D, E>>>(capacity = Channel.UNLIMITED)

	private val sharedIncomingEffects = effectChannel.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	// endregion

	private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

	// region data query

	private val routerQueryResponseChannel: MutableSharedFlow<D?> = MutableSharedFlow()

	private val outGoingQueryChannel: Channel<DataQuery> = Channel(capacity = Channel.BUFFERED)

	private val queryResponseChannel: Channel<QueryResponse<D>> =
		Channel(capacity = Channel.BUFFERED)

	private val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	private suspend fun processQuery(query: DataQuery) = withContext(dispatcher) {
		val dataHolder = transformerSet
			.filter { it.transmissionDataHolderState is HolderState.Initialized }
			.filter { if (query.dataOwner != null) query.dataOwner == it.transformerName else true }
			.find {
				(it.transmissionDataHolderState as HolderState.Initialized)
					.valueSet.contains(query.type)
			}
		if (query.sender == routerName) {
			routerQueryResponseChannel.emit(dataHolder?.holderData?.value?.get(query.type))
		} else {
			queryResponseChannel.trySend(
				QueryResponse(
					owner = query.sender,
					data = dataHolder?.holderData?.value?.get(query.type)
				)
			)
		}
	}

	// endregion

	fun initialize(
		onData: ((D) -> Unit),
		onEffect: (E) -> Unit = {},
	) {
		if (transformerSet.isEmpty()) {
			throw IllegalStateException("transformerSet should not be empty")
		}
		initializationJob = coroutineScope.launch {
			launch { sharedIncomingEffects.collect { onEffect(it.effect) } }
			launch { outGoingQueryChannel.consumeAsFlow().collect { processQuery(it) } }
			launch { outGoingDataChannel.consumeAsFlow().collect { onData(it) } }
			transformerSet.map { transformer ->
				launch {
					transformer.initialize(
						incomingSignal = sharedIncomingSignals,
						incomingEffect = sharedIncomingEffects,
						outGoingData = outGoingDataChannel,
						outGoingEffect = effectChannel,
						outGoingQuery = outGoingQueryChannel,
						incomingQueryResponse = incomingQueryResponse
					)
				}
			}
		}
	}

	fun clear() {
		initializationJob?.cancel()
		transformerSet.forEach { it.clear() }
	}

	@Suppress("UNCHECKED_CAST")
	override suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D? {
		outGoingQueryChannel.trySend(
			DataQuery(
				sender = routerName,
				type = type.simpleName.orEmpty()
			)
		)
		return routerQueryResponseChannel.filterIsInstance(type).firstOrNull()
	}

	override suspend fun <D : Transmission.Data, T : Transformer<D, E>> queryData(
		type: KClass<D>,
		owner: KClass<T>
	): D? {
		outGoingQueryChannel.trySend(
			DataQuery(
				sender = routerName,
				dataOwner = owner.simpleName.orEmpty(),
				type = type.simpleName.orEmpty()
			)
		)
		return routerQueryResponseChannel.filterIsInstance(type).firstOrNull()
	}
}

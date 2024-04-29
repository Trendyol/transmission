package com.trendyol.transmission

import com.trendyol.transmission.transformer.HolderState
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.DataQuery
import com.trendyol.transmission.transformer.query.QueryResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

class TransmissionRouter<D : Transmission.Data, E : Transmission.Effect>(
	private val transformerSet: Set<Transformer<D, E>>,
	private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

	private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

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

	private val effectChannel = Channel<E>(capacity = Channel.UNLIMITED)

	private val sharedIncomingEffects = effectChannel.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	// endregion

	private val outGoingDataChannel = Channel<D>(capacity = Channel.BUFFERED)

	// region data query

	private val outGoingQueryChannel: Channel<DataQuery> = Channel(capacity = Channel.BUFFERED)

	private val queryResponseChannel: Channel<QueryResponse<D>> =
		Channel(capacity = Channel.BUFFERED)

	private val incomingQueryResponse = queryResponseChannel.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	private suspend fun processQuery(query: DataQuery) = withContext(dispatcher) {
		val dataHolder = transformerSet
			.filter { it.transmissionDataHolderState != HolderState.Undefined }
			.find {
				it.transmissionDataHolderState is HolderState.Initialized &&
					(it.transmissionDataHolderState as HolderState.Initialized).value == query.type
			}
		queryResponseChannel.trySend(QueryResponse(query.sender, dataHolder?.holderData?.value))
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
			launch { sharedIncomingEffects.collect { onEffect(it) } }
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

}

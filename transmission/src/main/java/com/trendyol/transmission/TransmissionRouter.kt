package com.trendyol.transmission

import com.trendyol.transmission.transformer.Transformer
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

typealias DefaultTransmissionRouter = TransmissionRouter<Transmission.Data, Transmission.Effect>

class TransmissionRouter<D: Transmission.Data, E: Transmission.Effect>(
	private val transformerSet: Set<Transformer<D,E>>,
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

	fun initialize(
		onData: ((D) -> Unit),
		onEffect: (E) -> Unit = {},
	) {
		if (transformerSet.isEmpty()) {
			throw IllegalStateException("transformerSet should not be empty")
		}
		initializationJob = coroutineScope.launch {
			launch { sharedIncomingEffects.collect { onEffect(it) } }
			launch { outGoingDataChannel.consumeAsFlow().collect { onData(it) } }
			transformerSet.map { transformer ->
				launch {
					transformer.initialize(
						incomingSignal = sharedIncomingSignals,
						incomingEffect = sharedIncomingEffects,
						outGoingData = outGoingDataChannel,
						outGoingEffect = effectChannel
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

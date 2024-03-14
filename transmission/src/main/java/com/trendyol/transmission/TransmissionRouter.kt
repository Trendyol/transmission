package com.trendyol.transmission

import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class TransmissionRouter(private val transformerSet: Set<Transformer>) {

	private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

	private val effectChannel = Channel<Transmission.Effect>(capacity = Channel.UNLIMITED)

	private val sharedIncomingEffects = effectChannel.receiveAsFlow()
		.shareIn(coroutineScope, SharingStarted.Lazily)

	// endregion

	private val outGoingDataChannel = Channel<Transmission.Data>(capacity = Channel.BUFFERED)

	suspend fun initialize(
		onData: ((Transmission.Data) -> Unit),
		onEffect: (Transmission.Effect) -> Unit = {},
	) {
		initializationJob = coroutineScope.launch {
			launch { sharedIncomingEffects.onEach { onEffect(it) }.collect() }
			launch { outGoingDataChannel.consumeAsFlow().onEach { onData(it) }.collect() }
			launch {
				transformerSet.forEach { transformer ->
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

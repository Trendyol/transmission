package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

abstract class Transformer {

	private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

	private val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.UNLIMITED)
	private val effectChannel: Channel<Transmission.Effect> = Channel(capacity = Channel.UNLIMITED)

	private val jobMap: MutableMap<JobType, Job?> = mutableMapOf()

	protected fun updateJobMap(key: JobType, newJob : () -> Job) {
		jobMap.update(key, newJob)
	}

	protected fun <T : Transmission.Data?> MutableStateFlow<T>.reflectUpdates(): StateFlow<T> {
		jobMap.update(JobType("data")) {
			coroutineScope.launch {
				this@reflectUpdates.collect { sendData(it) }
			}
		}
		return this.asStateFlow()
	}

	abstract suspend fun onSignal(signal: Transmission.Signal)
	abstract suspend fun onEffect(effect: Transmission.Effect)

	suspend fun initialize(
		incomingSignal: SharedFlow<Transmission.Signal>,
		incomingEffect: SharedFlow<Transmission.Effect>,
		outGoingData: SendChannel<Transmission.Data>,
		outGoingEffect: SendChannel<Transmission.Effect>,
	) {
		jobMap.update(JobType("initialization")) {
			coroutineScope.launch {
				launch { incomingSignal.onEach { onSignal(it) }.collect() }
				launch { incomingEffect.onEach { onEffect(it) }.collect() }
				launch { dataChannel.receiveAsFlow().onEach { outGoingData.trySend(it) }.collect() }
				launch {
					effectChannel.receiveAsFlow().onEach { outGoingEffect.trySend(it) }.collect()
				}
			}
		}
	}

	private fun sendData(data: Transmission.Data?) {
		data?.let { dataChannel.trySend(it) }
	}

	protected fun sendEffect(effect: Transmission.Effect) {
		effectChannel.trySend(effect)
	}

	fun clear() {
		jobMap.clearJobs()
	}
}
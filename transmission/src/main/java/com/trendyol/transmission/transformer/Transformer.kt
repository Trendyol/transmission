package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.HandlerScope
import com.trendyol.transmission.transformer.handler.SignalHandler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

open class Transformer(private val dispatcher: CoroutineDispatcher = Dispatchers.Default) {

	open val signalHandler: SignalHandler? = null

	open val effectHandler: EffectHandler? = null

	private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

	private val handlerScope: HandlerScope = object : HandlerScope {
		override fun publishData(data: Transmission.Data?) {
			data?.let { dataChannel.trySend(it) }
		}

		override fun publishEffect(effect: Transmission.Effect) {
			effectChannel.trySend(effect)
		}

	}

	fun initialize(
		incomingSignal: SharedFlow<Transmission.Signal>,
		incomingEffect: SharedFlow<Transmission.Effect>,
		outGoingData: SendChannel<Transmission.Data>,
		outGoingEffect: SendChannel<Transmission.Effect>,
	) {
		jobMap.update("initialization") {
			coroutineScope.launch {
				launch {
					incomingSignal.collect {
						signalHandler?.apply { with(handlerScope) { onSignal(it) } }
					}
				}
				launch {
					incomingEffect.collect {
						effectHandler?.apply { with(handlerScope) { onEffect(it) } }
					}
				}
				launch { dataChannel.receiveAsFlow().collect { outGoingData.trySend(it) } }
				launch {
					effectChannel.receiveAsFlow().collect { outGoingEffect.trySend(it) }
				}
			}
		}
	}

	private val dataChannel: Channel<Transmission.Data> = Channel(capacity = Channel.UNLIMITED)
	private val effectChannel: Channel<Transmission.Effect> = Channel(capacity = Channel.UNLIMITED)

	private val jobMap: MutableMap<JobType, Job?> = mutableMapOf()

	protected fun updateJobMap(key: String, newJob: () -> Job) {
		jobMap.update(key, newJob)
	}

	protected fun <T : Transmission.Data?> MutableStateFlow<T>.reflectUpdates(): MutableStateFlow<T> {
		jobMap.update("data") {
			coroutineScope.launch {
				this@reflectUpdates.collect { it?.let { dataChannel.trySend(it) } }
			}
		}
		return this
	}

	fun clear() {
		jobMap.clearJobs()
	}
}

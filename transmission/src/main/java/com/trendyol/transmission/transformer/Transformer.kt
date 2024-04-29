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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias DefaultTransformer = Transformer<Transmission.Data, Transmission.Effect>

open class Transformer<D : Transmission.Data, E : Transmission.Effect>(
	dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

	private val dataChannel: Channel<D> = Channel(capacity = Channel.UNLIMITED)
	private val effectChannel: Channel<E> = Channel(capacity = Channel.UNLIMITED)

	private val jobList: MutableList<Job?> = mutableListOf()

	open val signalHandler: SignalHandler<D, E>? = null

	open val effectHandler: EffectHandler<D, E>? = null

	private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

	private val handlerScope: HandlerScope<D, E> = object : HandlerScope<D, E> {
		override fun publishData(data: D?) {
			data?.let { dataChannel.trySend(it) }
		}

		override fun publishEffect(effect: E) {
			effectChannel.trySend(effect)
		}
	}

	fun initialize(
		incomingSignal: SharedFlow<Transmission.Signal>,
		incomingEffect: SharedFlow<Transmission.Effect>,
		outGoingData: SendChannel<D>,
		outGoingEffect: SendChannel<E>,
	) {
		jobList += coroutineScope.launch {
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

	protected inner class TransmissionDataHolder<T : D?>(initialValue: T) {

		private val holder = MutableStateFlow(initialValue)

		val value: T
			get() = holder.value

		init {
			jobList += coroutineScope.launch {
				holder.collect { it?.let { dataChannel.trySend(it) } }
			}
		}

		fun update(updater: (T) -> T) {
			holder.update(updater)
		}
	}

	fun clear() {
		jobList.clearJobs()
	}

	// region handler extensions

	inline fun <reified S : Transmission.Signal> Transformer<D, E>.buildTypedSignalHandler(
		crossinline onSignal: suspend HandlerScope<D, E>.(signal: S) -> Unit
	): SignalHandler<D, E> {
		return SignalHandler { incomingSignal ->
			incomingSignal
				.takeIf { it is S }
				?.let { signal -> onSignal(signal as S) }
		}
	}

	inline fun <reified E : Transmission.Effect> Transformer<D, E>.buildTypedEffectHandler(
		crossinline onEffect: suspend HandlerScope<D, E>.(effect: E) -> Unit
	): EffectHandler<D, E> {
		return EffectHandler { incomingEffect ->
			incomingEffect
				.takeIf { it is E }
				?.let { effect -> onEffect(effect as E) }
		}
	}

	// endregion
}

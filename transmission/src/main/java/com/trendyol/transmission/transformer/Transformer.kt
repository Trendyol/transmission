package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.query.DataQuery
import com.trendyol.transmission.transformer.query.QueryResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

typealias DefaultTransformer = Transformer<Transmission.Data, Transmission.Effect>

open class Transformer<D : Transmission.Data, E : Transmission.Effect>(
	dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

	val transformerName: String = this::class.java.simpleName

	private val dataChannel: Channel<D> = Channel(capacity = Channel.UNLIMITED)
	private val effectChannel: Channel<EffectWrapper<E, D, Transformer<D, E>>> =
		Channel(capacity = Channel.UNLIMITED)

	private val outGoingQueryChannel: Channel<DataQuery> = Channel()
	private val queryResponseChannel: Channel<D?> = Channel()

	private val holderDataReference: MutableStateFlow<MutableMap<String, D?>> =
		MutableStateFlow(mutableMapOf())
	val holderData = holderDataReference.asStateFlow()

	protected var internalTransmissionHolderSet: HolderState = HolderState.Undefined
	val transmissionDataHolderState: HolderState
		get() = internalTransmissionHolderSet

	open val signalHandler: SignalHandler<D, E>? = null

	open val effectHandler: EffectHandler<D, E>? = null

	protected val transformerScope = CoroutineScope(SupervisorJob() + dispatcher)

	private val communicationScope: CommunicationScope<D, E> = object : CommunicationScope<D, E> {
		override fun publishData(data: D?) {
			data?.let { dataChannel.trySend(it) }
		}

		override fun publishEffect(effect: E) {
			effectChannel.trySend(EffectWrapper(effect))
		}

		override fun sendEffect(effect: E, to: KClass<out Transformer<D, E>>) {
			effectChannel.trySend(EffectWrapper(effect, to))
		}

		@Suppress("UNCHECKED_CAST")
		override suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D? {
			outGoingQueryChannel.trySend(
				DataQuery(
					sender = transformerName,
					type = type.simpleName.orEmpty()
				)
			)
			return queryResponseChannel.receive() as? D
		}

		@Suppress("UNCHECKED_CAST")
		override suspend fun <D : Transmission.Data, TD : Transmission.Data, T : Transformer<TD, E>> queryData(
			type: KClass<D>,
			owner: KClass<out T>
		): D? {
			outGoingQueryChannel.trySend(
				DataQuery(
					sender = transformerName,
					dataOwner = owner.simpleName.orEmpty(),
					type = type.simpleName.orEmpty()
				)
			)
			return queryResponseChannel.receive() as? D
		}
	}

	protected suspend fun communicate(scope: suspend CommunicationScope<D,E>.() -> Unit) {
		communicationScope.apply { scope() }
	}

	fun initialize(
		incomingSignal: SharedFlow<Transmission.Signal>,
		incomingEffect: SharedFlow<EffectWrapper<E, D, Transformer<D, E>>>,
		incomingQueryResponse: SharedFlow<QueryResponse<D>>,
		outGoingData: SendChannel<D>,
		outGoingEffect: SendChannel<EffectWrapper<E, D, Transformer<D, E>>>,
		outGoingQuery: SendChannel<DataQuery>,
	) {
		transformerScope.launch {
			launch {
				incomingSignal.collect {
					signalHandler?.apply { with(communicationScope) { onSignal(it) } }
				}
			}
			launch {
				incomingEffect
					.filterNot { it.effect is RouterPayloadEffect }
					.collect { incomingEffect ->
						val effectToProcess = incomingEffect.takeIf {
							incomingEffect.to == null ||
								incomingEffect.to == this@Transformer::class
						}?.effect ?: return@collect

						effectHandler?.apply {
							with(communicationScope) {
								onEffect(effectToProcess)
							}
						}
					}
			}
			launch {
				incomingQueryResponse.filter { it.owner == transformerName }.collect {
					queryResponseChannel.trySend(it.data)
				}
			}
			launch { outGoingQueryChannel.receiveAsFlow().collect { outGoingQuery.trySend(it) } }
			launch { dataChannel.receiveAsFlow().collect { outGoingData.trySend(it) } }
			launch {
				effectChannel.receiveAsFlow().collect { outGoingEffect.trySend(it) }
			}
		}
	}

	fun clear() {
		transformerScope.cancel()
	}

	// region DataHolder

	inner class TransmissionDataHolder<T : D?>(initialValue: T, publishUpdates: Boolean) {

		private val holder = MutableStateFlow(initialValue)

		val value: T
			get() = holder.value

		init {
			transformerScope.launch {
				holder.collect {
					it?.let { holderData ->
						holderDataReference.update { holderDataReference ->
							holderDataReference[holderData::class.java.simpleName] = holderData
							holderDataReference
						}
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
	protected inline fun <reified T : D?> Transformer<D, E>.buildDataHolder(
		initialValue: T,
		publishUpdates: Boolean = true,
	): TransmissionDataHolder<T> {
		val dataHolderToTrack = T::class.java.simpleName
		when (internalTransmissionHolderSet) {
			is HolderState.Initialized -> {
				val currentSet =
					(internalTransmissionHolderSet as HolderState.Initialized).valueSet
				require(!currentSet.contains(dataHolderToTrack)) {
					"Multiple data holders with the same type is not allowed: $dataHolderToTrack"
				}
				internalTransmissionHolderSet =
					HolderState.Initialized(currentSet + dataHolderToTrack)
			}

			HolderState.Undefined -> {
				internalTransmissionHolderSet =
					HolderState.Initialized(setOf(dataHolderToTrack))
			}
		}
		return TransmissionDataHolder(initialValue, publishUpdates)
	}

	// endregion

	// region handler extensions

	inline fun <reified S : Transmission.Signal> Transformer<D, E>.buildTypedSignalHandler(
		crossinline onSignal: suspend CommunicationScope<D, E>.(signal: S) -> Unit
	): SignalHandler<D, E> {
		return SignalHandler { incomingSignal ->
			incomingSignal
				.takeIf { it is S }
				?.let { signal -> onSignal(signal as S) }
		}
	}

	inline fun <reified E : Transmission.Effect> Transformer<D, E>.buildTypedEffectHandler(
		crossinline onEffect: suspend CommunicationScope<D, E>.(effect: E) -> Unit
	): EffectHandler<D, E> {
		return EffectHandler { incomingEffect ->
			incomingEffect
				.takeIf { it is E }
				?.let { effect -> onEffect(effect as E) }
		}
	}

	// endregion

}

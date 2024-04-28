package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.data.TestData
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericSignalHandler
import kotlinx.coroutines.CoroutineDispatcher

open class FakeTransformer(dispatcher: CoroutineDispatcher) : Transformer(dispatcher) {
	val signalList = mutableListOf<Transmission.Signal>()
	val effectList = mutableListOf<Transmission.Effect>()

	private val holder = TransmissionDataHolder<TestData?>(null)

	override val signalHandler: SignalHandler = buildGenericSignalHandler { signal ->
		signalList.add(signal)
		publishEffect(TestEffect)
		holder.update { TestData("update with ${this.javaClass.simpleName}") }
	}

	override val effectHandler: EffectHandler = buildGenericEffectHandler { effect ->
		effectList.add(effect)
	}
}

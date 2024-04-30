package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.transformer.data.TestData
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.handler.buildGenericEffectHandler
import com.trendyol.transmission.transformer.handler.buildGenericSignalHandler
import kotlinx.coroutines.CoroutineDispatcher

open class FakeTransformer(dispatcher: CoroutineDispatcher) : DefaultTransformer(dispatcher) {
	val signalList = mutableListOf<Transmission.Signal>()
	val effectList = mutableListOf<Transmission.Effect>()

	private val holder = TransmissionDataHolder<TestData?>(null)

	override val signalHandler = buildGenericSignalHandler { signal ->
		signalList.add(signal)
		publishEffect(TestEffect)
		publishEffect(RouterPayloadEffect(""))
		holder.update { TestData("update with ${this@FakeTransformer.javaClass.simpleName}") }
	}

	override val effectHandler = buildGenericEffectHandler { effect ->
		effectList.add(effect)
	}
}

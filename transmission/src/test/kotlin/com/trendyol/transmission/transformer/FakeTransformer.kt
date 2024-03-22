package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.handler.EffectHandler
import com.trendyol.transmission.transformer.handler.SignalHandler

open class FakeTransformer : Transformer() {
	val signalList = mutableListOf<Transmission.Signal>()
	val effectList = mutableListOf<Transmission.Effect>()


	override val signalHandler: SignalHandler = SignalHandler { signal ->
		signalList.add(signal)
		sendEffect(TestEffect)
	}

	override val effectHandler: EffectHandler = EffectHandler { effect ->
		effectList.add(effect)
	}
}

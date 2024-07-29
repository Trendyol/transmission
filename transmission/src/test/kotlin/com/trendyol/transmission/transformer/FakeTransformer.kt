package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.data.TestData
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.data.TestSignal
import com.trendyol.transmission.transformer.dataholder.buildDataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlerRegistry
import com.trendyol.transmission.transformer.handler.registerEffect
import com.trendyol.transmission.transformer.handler.registerSignal
import kotlinx.coroutines.CoroutineDispatcher

open class FakeTransformer(dispatcher: CoroutineDispatcher) : Transformer(dispatcher) {
    val signalList = mutableListOf<Transmission.Signal>()
    val effectList = mutableListOf<Transmission.Effect>()

    private val holder = buildDataHolder<TestData?>(null)

    override val handlerRegistry: HandlerRegistry = handlerRegistry {
        registerSignal<TestSignal> { signal ->
            signalList.add(signal)
            publish(TestEffect)
            publish(RouterEffect(""))
            holder.update { TestData("update with ${this@FakeTransformer.javaClass.simpleName}") }
        }
        registerEffect<TestEffect> { effect ->
            effectList.add(effect)
        }
    }
}

package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.data.TestData
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.data.TestSignal
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.effect
import com.trendyol.transmission.transformer.handler.handlers
import kotlinx.coroutines.CoroutineDispatcher

open class FakeTransformer(dispatcher: CoroutineDispatcher) : Transformer(dispatcher) {
    val signalList = mutableListOf<Transmission.Signal>()
    val effectList = mutableListOf<Transmission.Effect>()

    private val holder = dataHolder<TestData?>(null)

    override val handlers: HandlerRegistry = handlers {
        effect<TestSignal> { signal ->
            signalList.add(signal)
            publish(TestEffect)
            publish(RouterEffect(""))
            holder.update { TestData("update with ${this@FakeTransformer.javaClass.simpleName}") }
        }
        effect<TestEffect> { effect ->
            effectList.add(effect)
        }
    }
}

package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
fun Transformer.testWithEffect(
    orderedInitialProcessing: List<Transmission> = emptyList<Transmission>(),
    effect: Transmission.Effect,
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    runTest {
        val registryImpl = RegistryScopeImpl().apply(registry)
        val testRouter = TestRouter(registryImpl, this@testWithEffect, UnconfinedTestDispatcher())
        val dataStream: MutableList<Transmission.Data> = mutableListOf()
        val effectStream: MutableList<EffectWrapper> = mutableListOf()
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                testRouter.dataStream.toList(dataStream)
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                testRouter.effectStream.toList(effectStream)
            }
            val testScope = object : TransformerTestScope {
                override val dataStream: List<Transmission.Data> = dataStream
                override val effectStream: List<EffectWrapper> = effectStream
            }
            orderedInitialProcessing.forEach {
                when (it) {
                    is Transmission.Data -> throw IllegalArgumentException("Transmission.Data should not be sent for processing")
                    is Transmission.Effect -> testRouter.sendEffect(it)
                    is Transmission.Signal -> testRouter.sendSignal(it)
                }
                this@testWithEffect.waitProcessingToFinish()
            }
            testRouter.sendEffect(effect)
            this@testWithEffect.waitProcessingToFinish()
            testScope.scope(this)
        } finally {
            advanceUntilIdle()
            testRouter.clear()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Transformer.testWithSignal(
    orderedInitialProcessing: List<Transmission> = emptyList<Transmission>(),
    signal: Transmission.Signal,
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    runTest {
        val registryImpl = RegistryScopeImpl().apply(registry)
        val testRouter = TestRouter(registryImpl, this@testWithSignal, UnconfinedTestDispatcher())
        val dataStream: MutableList<Transmission.Data> = mutableListOf()
        val effectStream: MutableList<EffectWrapper> = mutableListOf()
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                testRouter.dataStream.toList(dataStream)
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                testRouter.effectStream.toList(effectStream)
            }
            val testScope = object : TransformerTestScope {
                override val dataStream: List<Transmission.Data> = dataStream
                override val effectStream: List<EffectWrapper> = effectStream
            }
            orderedInitialProcessing.forEach {
                when (it) {
                    is Transmission.Data -> throw IllegalArgumentException("Transmission.Data should not be sent for processing")
                    is Transmission.Effect -> testRouter.sendEffect(it)
                    is Transmission.Signal -> testRouter.sendSignal(it)
                }
                this@testWithSignal.waitProcessingToFinish()
            }
            testRouter.sendSignal(signal)
            this@testWithSignal.waitProcessingToFinish()
            testScope.scope(this)
        } finally {
            advanceUntilIdle()
            testRouter.clear()
        }
    }
}

suspend fun Transformer.waitProcessingToFinish() {
    currentSignalProcessing?.join()
    currentEffectProcessing?.join()
}

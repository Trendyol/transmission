package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest


@OptIn(ExperimentalCoroutinesApi::class)
fun Transformer.testWith(
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    runTest {
        val registryImpl = RegistryScopeImpl().apply(registry)
        val testRouter = TestRouter(registryImpl, this@testWith, UnconfinedTestDispatcher())
        val dataStream: MutableList<Transmission.Data> = mutableListOf()
        val effectStream: MutableList<EffectWrapper> = mutableListOf()
        try {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
               testRouter.dataStream.toList(dataStream)
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                testRouter.effectStream.toList(effectStream)
            }
            val testScope = object: TransformerTestScope {
                override val dataStream: List<Transmission.Data> = dataStream
                override val effectStream: List<EffectWrapper> = effectStream
                override fun sendSignal(signal: Transmission.Signal) {
                    testRouter.sendSignal(signal)
                }

                override fun sendEffect(effect: Transmission.Effect) {
                    testRouter.sendEffect(effect)
                }

            }
            advanceUntilIdle()
            testScope.scope(this)
        } finally {
            advanceUntilIdle()
            testRouter.clear()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun testTransformer(
    transformerProvider: CoroutineDispatcher.() -> Transformer,
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    runTest {
        val registryImpl = RegistryScopeImpl().apply(registry)
        val testRouter = TestRouter(
            registryImpl, transformerProvider.invoke(
                UnconfinedTestDispatcher(testScheduler)
            ), UnconfinedTestDispatcher(testScheduler)
        )
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
                override fun sendSignal(signal: Transmission.Signal) {
                    testRouter.sendSignal(signal)
                }

                override fun sendEffect(effect: Transmission.Effect) {
                    testRouter.sendEffect(effect)
                }

            }
            advanceUntilIdle()
            testScope.scope(this)
        } finally {
            advanceUntilIdle()
            testRouter.clear()
        }
    }
}
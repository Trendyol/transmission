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
class TestSuite {
    private var orderedInitialProcessing: List<Transmission> = emptyList()
    private var transformer: Transformer? = null
    private lateinit var router: TestRouter

    fun initialize(transformer: Transformer): TestSuite {
        this.transformer = transformer
        router = TestRouter(transformer, UnconfinedTestDispatcher())
        return this
    }

    fun register(registry: RegistryScope.() -> Unit = {}): TestSuite {
        router.registry = RegistryScopeImpl().apply(registry)
        return this
    }

    fun processBeforeTesting(vararg transmissions: Transmission): TestSuite {
        orderedInitialProcessing = transmissions.toList()
        return this
    }

    @PublishedApi
    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun runTest(
        transmission: Transmission,
        scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
    ) {

        runTest {
            val dataStream: MutableList<Transmission.Data> = mutableListOf()
            val effectStream: MutableList<EffectWrapper> = mutableListOf()
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router.dataStream.toList(dataStream)
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router.effectStream.toList(effectStream)
                }
                val testScope = object : TransformerTestScope {
                    override val dataStream: List<Transmission.Data> = dataStream
                    override val effectStream: List<EffectWrapper> = effectStream
                }
                orderedInitialProcessing.forEach {
                    when (it) {
                        is Transmission.Data -> throw IllegalArgumentException("Transmission.Data should not be sent for processing")
                        is Transmission.Effect -> router.sendEffect(it)
                        is Transmission.Signal -> router.sendSignal(it)
                    }
                    transformer?.waitProcessingToFinish()
                }
                if (transmission is Transmission.Signal) {
                    router.sendSignal(transmission)
                } else if (transmission is Transmission.Effect) {
                    router.sendEffect(transmission)
                }
                transformer?.waitProcessingToFinish()
                testScope.scope(this)
            } finally {
                advanceUntilIdle()
                router.clear()
            }
        }
    }
}

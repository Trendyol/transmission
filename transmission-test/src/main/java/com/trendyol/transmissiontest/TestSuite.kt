package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.RegistryScope
import com.trendyol.transmission.router.builder.TransmissionTestingRouterBuilder
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
    private var registryScope: RegistryScope.() -> Unit = {}
    private lateinit var router: TransmissionRouter

    fun initialize(transformer: Transformer): TestSuite {
        this.transformer = transformer
        return this
    }

    fun register(registry: RegistryScope.() -> Unit = {}): TestSuite {
        this.registryScope = registry
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
        router = TransmissionTestingRouterBuilder.build {
            addDispatcher(UnconfinedTestDispatcher())
            this@TestSuite.transformer?.let { addTransformerSet(setOf(it)) }
            testing(this@TestSuite.registryScope)
        }

        runTest {
            val dataStream: MutableList<Transmission.Data> = mutableListOf()
            val effectStream: MutableList<Transmission.Effect> = mutableListOf()
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router.dataStream.toList(dataStream)
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router.effectStream.toList(effectStream)
                }
                val testScope = object : TransformerTestScope {
                    override val dataStream: List<Transmission.Data> = dataStream
                    override val effectStream: List<Transmission.Effect> = effectStream
                }
                orderedInitialProcessing.forEach {
                    when (it) {
                        is Transmission.Data -> throw IllegalArgumentException("Transmission.Data should not be sent for processing")
                        is Transmission.Effect -> router.processEffect(it)
                        is Transmission.Signal -> router.processSignal(it)
                    }
                    transformer?.waitProcessingToFinish()
                }
                if (transmission is Transmission.Signal) {
                    router.processSignal(transmission)
                } else if (transmission is Transmission.Effect) {
                    router.processEffect(transmission)
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

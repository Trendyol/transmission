package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.builder.TransmissionRouterBuilder
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmissiontest.computation.ComputationTransformer
import com.trendyol.transmissiontest.computation.ComputationWithArgsTransformer
import com.trendyol.transmissiontest.data.DataTransformer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TestSuite {
    private var orderedInitialProcessing: MutableList<Transmission> = mutableListOf()
    private var transformer: Transformer? = null
    private lateinit var router: TransmissionRouter
    private val supplementaryTransformerSet: MutableList<Transformer> = mutableListOf()

    fun initialize(transformer: Transformer): TestSuite {
        this.transformer = transformer
        return this
    }

    fun <D : Transmission.Data?> registerData(
        contract: Contract.DataHolder<D>, data: () -> D
    ): TestSuite {
        supplementaryTransformerSet += DataTransformer(contract, data)
        return this
    }

    fun <C : Contract.Computation<D?>, D : Any> registerComputation(
        contract: C, data: () -> D?
    ): TestSuite {
        supplementaryTransformerSet += ComputationTransformer(contract, data)
        return this
    }

    fun <C : Contract.ComputationWithArgs<A,D?>, D : Any, A: Any> registerComputation(
        contract: C, data: () -> D?
    ): TestSuite {
        supplementaryTransformerSet += ComputationWithArgsTransformer(contract, data)
        return this
    }

    fun processBeforeTesting(vararg transmissions: Transmission): TestSuite {
        orderedInitialProcessing += transmissions.toList()
        return this
    }

    @PublishedApi
    @OptIn(ExperimentalCoroutinesApi::class)
    internal fun runTest(
        transmission: Transmission,
        scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
    ) {
        router = TransmissionRouterBuilder.build {
            addDispatcher(UnconfinedTestDispatcher())
            this@TestSuite.transformer?.let {
                addTransformerSet((listOf(it) + supplementaryTransformerSet).toSet())
            }
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

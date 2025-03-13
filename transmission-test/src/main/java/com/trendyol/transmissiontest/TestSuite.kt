package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.router.streamData
import com.trendyol.transmission.router.streamEffect
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
@Deprecated(
    message = "Use TransmissionTest instead. See Transformer.test() extension function.",
    replaceWith =
        ReplaceWith(
            "TransmissionTest.forTransformer(transformer)",
            "com.trendyol.transmissiontest.TransmissionTest"
        )
)
class TestSuite {
    private var orderedInitialProcessing: MutableList<Transmission> = mutableListOf()
    private var transformer: Transformer? = null
    private lateinit var router: TransmissionRouter
    private val supplementaryTransformerSet: MutableList<Transformer> = mutableListOf()

    @Deprecated(
        message = "Use TransmissionTest.forTransformer() instead",
        replaceWith =
            ReplaceWith(
                "TransmissionTest.forTransformer(transformer)",
                "com.trendyol.transmissiontest.TransmissionTest"
            )
    )
    fun initialize(transformer: Transformer): TestSuite {
        this.transformer = transformer
        return this
    }

    @Deprecated(
        message = "Use TransmissionTest.withData() instead",
        replaceWith =
            ReplaceWith(
                "withData(contract, data)",
                "com.trendyol.transmissiontest.TransmissionTest"
            )
    )
    fun <D : Transmission.Data?> registerData(
        contract: Contract.DataHolder<D>,
        data: () -> D
    ): TestSuite {
        supplementaryTransformerSet += DataTransformer(contract, data)
        return this
    }

    @Deprecated(
        message = "Use TransmissionTest.withComputation() instead",
        replaceWith =
            ReplaceWith(
                "withComputation(contract, data)",
                "com.trendyol.transmissiontest.TransmissionTest"
            )
    )
    fun <C : Contract.Computation<D?>, D : Any> registerComputation(
        contract: C,
        data: () -> D?
    ): TestSuite {
        supplementaryTransformerSet += ComputationTransformer(contract, data)
        return this
    }

    @Deprecated(
        message = "Use TransmissionTest.withComputation() instead",
        replaceWith =
            ReplaceWith(
                "withComputation(contract, data)",
                "com.trendyol.transmissiontest.TransmissionTest"
            )
    )
    fun <C : Contract.ComputationWithArgs<A, D?>, D : Any, A : Any> registerComputation(
        contract: C,
        data: () -> D?
    ): TestSuite {
        supplementaryTransformerSet += ComputationWithArgsTransformer(contract, data)
        return this
    }

    @Deprecated(
        message = "Use TransmissionTest.withInitialProcessing() instead",
        replaceWith =
            ReplaceWith(
                "withInitialProcessing(*transmissions)",
                "com.trendyol.transmissiontest.TransmissionTest"
            )
    )
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
        router = TransmissionRouter {
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
                    router.streamData().toList(dataStream)
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router.streamEffect().toList(effectStream)
                }
                val testScope =
                    object : TransformerTestScope {
                        override val dataStream: List<Transmission.Data> = dataStream
                        override val effectStream: List<Transmission.Effect> = effectStream
                    }
                orderedInitialProcessing.forEach {
                    when (it) {
                        is Transmission.Data ->
                            throw IllegalArgumentException(
                                "Transmission.Data should not be sent for processing"
                            )

                        is Transmission.Effect -> router.process(it)
                        is Transmission.Signal -> router.process(it)
                    }
                    transformer?.waitProcessingToFinish()
                }
                if (transmission is Transmission.Signal) {
                    router.process(transmission)
                } else if (transmission is Transmission.Effect) {
                    router.process(transmission)
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

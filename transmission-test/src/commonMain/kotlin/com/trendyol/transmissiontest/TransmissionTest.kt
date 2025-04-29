package com.trendyol.transmissiontest

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.router.builder.TransmissionRouter
import com.trendyol.transmission.router.streamData
import com.trendyol.transmission.router.streamEffect
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmissiontest.checkpoint.CheckpointTransformer
import com.trendyol.transmissiontest.checkpoint.CheckpointWithArgs
import com.trendyol.transmissiontest.checkpoint.CheckpointWithArgsTransformer
import com.trendyol.transmissiontest.checkpoint.DefaultCheckPoint
import com.trendyol.transmissiontest.computation.ComputationTransformer
import com.trendyol.transmissiontest.computation.ComputationWithArgsTransformer
import com.trendyol.transmissiontest.data.DataTransformer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.jvm.JvmName

/**
 * Main entry point for testing Transmission components. Provides a DSL for setting up and testing
 * transformers with a fluent API.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransmissionTest
private constructor(
        private val transformer: Transformer,
        private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    private var router: TransmissionRouter? = null
    private val mockTransformers: MutableList<Transformer> = mutableListOf()
    private val initialTransmissions: MutableList<Transmission> = mutableListOf()
    private val orderedCheckpoints: MutableList<Contract.Checkpoint> = mutableListOf()

    /** Adds a mock for a data contract that will be provided during testing */
    fun <D : Transmission.Data?> withData(
            contract: Contract.DataHolder<D>,
            data: () -> D
    ): TransmissionTest {
        mockTransformers += DataTransformer(contract, data)
        return this
    }

    /** Adds a mock for a computation contract that will be provided during testing */
    fun <C : Contract.Computation<D?>, D : Any> withComputation(
            contract: C,
            data: () -> D?
    ): TransmissionTest {
        mockTransformers += ComputationTransformer(contract, data)
        return this
    }

    /** Adds a mock for a computation with args contract that will be provided during testing */
    fun <C : Contract.ComputationWithArgs<A, D?>, D : Any, A : Any> withComputation(
            contract: C,
            data: () -> D?
    ): TransmissionTest {
        mockTransformers += ComputationWithArgsTransformer(contract, data)
        return this
    }

    /** Adds a checkpoint with args that will be validated during testing */
    @ExperimentalTransmissionApi
    fun <C : Contract.Checkpoint.WithArgs<A>, A : Any> withCheckpoint(
            checkpoint: C,
            args: A
    ): TransmissionTest {
        mockTransformers += CheckpointWithArgsTransformer<C, A>(checkpoint, { args })
        orderedCheckpoints.plusAssign(checkpoint)
        return this
    }

    /** Adds a default checkpoint that will be validated during testing */
    @ExperimentalTransmissionApi
    fun withCheckpoint(checkpoint: Contract.Checkpoint.Default): TransmissionTest {
        mockTransformers += CheckpointTransformer({ checkpoint })
        orderedCheckpoints.plusAssign(checkpoint)
        return this
    }

    /** Sets up initial transmissions that should be processed before the test transmission */
    fun withInitialProcessing(vararg transmissions: Transmission): TransmissionTest {
        initialTransmissions.addAll(transmissions)
        return this
    }

    /** Runs a test with the given effect transmission */
    fun testEffect(effect: Transmission.Effect, assertions: suspend TestResult.() -> Unit) {
        runTest(effect, assertions)
    }

    /** Runs a test with the given signal transmission */
    fun testSignal(signal: Transmission.Signal, assertions: suspend TestResult.() -> Unit) {
        runTest(signal, assertions)
    }

    /** Internal method to run the actual test with any transmission type */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun runTest(transmission: Transmission, assertions: suspend TestResult.() -> Unit) {
        router = TransmissionRouter {
            addDispatcher(dispatcher)
            addTransformerSet((listOf(transformer) + mockTransformers).toSet())
        }

        runTest {
            val dataStream: MutableList<Transmission.Data> = mutableListOf()
            val effectStream: MutableList<Transmission.Effect> = mutableListOf()

            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router!!.streamData().toList(dataStream)
                }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    router!!.streamEffect().toList(effectStream)
                }

                // Process initial transmissions
                initialTransmissions.forEach {
                    when (it) {
                        is Transmission.Data ->
                                throw IllegalArgumentException(
                                        "Transmission.Data should not be sent for processing"
                                )
                        is Transmission.Effect -> router!!.process(it)
                        is Transmission.Signal -> router!!.process(it)
                    }
                    transformer.waitProcessingToFinish()
                }

                // Process the test transmission
                when (transmission) {
                    is Transmission.Signal -> router!!.process(transmission)
                    is Transmission.Effect -> router!!.process(transmission)
                    else ->
                            throw IllegalArgumentException(
                                    "Only Signal or Effect transmissions are supported for testing"
                            )
                }

                // Process checkpoints
                orderedCheckpoints.forEach {
                    when (it) {
                        is Contract.Checkpoint.Default -> router!!.process(DefaultCheckPoint)
                        is Contract.Checkpoint.WithArgs<*> ->
                                router!!.process(CheckpointWithArgs(it))
                    }
                }

                transformer.waitProcessingToFinish()

                // Run the assertions
                val testResult = TestResult(dataStream, effectStream)
                testResult.assertions()
            } finally {
                advanceUntilIdle()
                router?.clear()
            }
        }
    }

    /** Class holding the test results for assertions */
    class TestResult(
            val dataStream: List<Transmission.Data>,
            val effectStream: List<Transmission.Effect>
    ) {
        /** Returns the last data of a specific type */
        @JvmName("lastDataWithType")
        inline fun <reified T : Transmission.Data> lastData(): T? =
                dataStream.filterIsInstance<T>().lastOrNull()

        fun lastData(): Transmission.Data? = dataStream.lastOrNull()

        fun nthData(index: Int): Transmission.Data? = dataStream.getOrNull(index)

        /** Returns the last effect of a specific type */
        @JvmName("lastEffectWithType")
        inline fun <reified T : Transmission.Effect> lastEffect(): T? =
                effectStream.filterIsInstance<T>().lastOrNull()

        fun lastEffect(): Transmission.Effect? = effectStream.lastOrNull()

        fun nthEffect(index: Int): Transmission.Effect? = effectStream.getOrNull(index)

        /** Returns all data of a specific type */
        inline fun <reified T : Transmission.Data> allData(): List<T> =
                dataStream.filterIsInstance<T>()

        /** Returns all effects of a specific type */
        inline fun <reified T : Transmission.Effect> allEffects(): List<T> =
                effectStream.filterIsInstance<T>()

        /** Finds the first data of a specific type that matches the predicate */
        inline fun <reified T : Transmission.Data> findData(predicate: (T) -> Boolean): T? =
                dataStream.filterIsInstance<T>().firstOrNull(predicate)

        /** Finds the first effect of a specific type that matches the predicate */
        inline fun <reified T : Transmission.Effect> findEffect(predicate: (T) -> Boolean): T? =
                effectStream.filterIsInstance<T>().firstOrNull(predicate)

        /** Finds all data of a specific type that match the predicate */
        inline fun <reified T : Transmission.Data> findAllData(predicate: (T) -> Boolean): List<T> =
                dataStream.filterIsInstance<T>().filter(predicate)

        /** Finds all effects of a specific type that match the predicate */
        inline fun <reified T : Transmission.Effect> findAllEffects(
                predicate: (T) -> Boolean
        ): List<T> = effectStream.filterIsInstance<T>().filter(predicate)

        /** Checks if any data of type T exists in the data stream */
        inline fun <reified T : Transmission.Data> hasData(): Boolean = dataStream.any { it is T }

        /** Checks if any effect of type T exists in the effect stream */
        inline fun <reified T : Transmission.Effect> hasEffect(): Boolean =
                effectStream.any { it is T }
    }

    companion object {
        /** Creates a new TransmissionTest for the given transformer */
        fun forTransformer(transformer: Transformer): TransmissionTest {
            return TransmissionTest(transformer)
        }

        /** Creates a new TransmissionTest for the given transformer with a custom dispatcher */
        fun forTransformer(transformer: Transformer, dispatcher: TestDispatcher): TransmissionTest {
            return TransmissionTest(transformer, dispatcher)
        }
    }
}

// Extension functions for more readable tests
fun Transformer.test(): TransmissionTest {
    return TransmissionTest.forTransformer(this)
}

fun Transformer.test(dispatcher: TestDispatcher): TransmissionTest {
    return TransmissionTest.forTransformer(this, dispatcher)
}

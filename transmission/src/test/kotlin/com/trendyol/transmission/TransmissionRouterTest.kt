package com.trendyol.transmission

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.transformer.FakeTransformer
import com.trendyol.transmission.transformer.TestTransformer1
import com.trendyol.transmission.transformer.TestTransformer2
import com.trendyol.transmission.transformer.TestTransformer3
import com.trendyol.transmission.transformer.data.TestData
import com.trendyol.transmission.transformer.data.TestEffect
import com.trendyol.transmission.transformer.data.TestSignal
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TransmissionRouterTest {

	private lateinit var sut: TransmissionRouter

	@get:Rule
	val testCoroutineRule = TestCoroutineRule()

	private val testDispatcher = UnconfinedTestDispatcher()

	@Test
	fun `GIVEN Router with no transformers, WHEN initialize is called, THEN router should throw IllegalStateException`() =
		runTest {
			// Given
			try {
				// When
				sut = TransmissionRouter(setOf())
			} catch (e: IllegalArgumentException) {
				// Then
				assertEquals(e.message, "transformerSet should not be empty")
			}
		}

	@Test
	fun `GIVEN Router with one transformer, WHEN initialize is called, THEN router should not throw IllegalStateException`() =
		runTest {
			// Given
			val exception = try {
				// When
				sut = TransmissionRouter(setOf(FakeTransformer(testDispatcher)))
				null
			} catch (e: IllegalStateException) {
				e
			}
			// Then
			assertEquals(exception, null)
		}

	@Test
	fun `GIVEN initialized Router with one Transformer, WHEN processSignal is called, THEN transformer should contain the signal`() {
		// Given
		val transformer = FakeTransformer(testDispatcher)
		sut = TransmissionRouter(setOf(transformer), testDispatcher)
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(transformer.signalList.last(), TestSignal)
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all transformers should contain the signal`() {
		// Given
		val transformer1 = TestTransformer1(testDispatcher)
		val transformer2 = TestTransformer2(testDispatcher)
		val transformer3 = TestTransformer3(testDispatcher)
		sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(transformer1.signalList.last(), TestSignal)
		assertEquals(transformer2.signalList.last(), TestSignal)
		assertEquals(transformer3.signalList.last(), TestSignal)
	}

	/**
	 * This test depends on the specific implementation of [FakeTransformer]. Added this to check
	 * the effect broadcast.
	 */
	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all transformers should contain the FakeTransformer's Signal`() {
		// Given
		val transformer1 = TestTransformer1(testDispatcher)
		val transformer2 = TestTransformer2(testDispatcher)
		val transformer3 = TestTransformer3(testDispatcher)
		sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(transformer1.effectList.last(), TestEffect)
		assertEquals(transformer2.effectList.last(), TestEffect)
		assertEquals(transformer3.effectList.last(), TestEffect)
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all effects should be sent through onEffect`() = runTest {
		turbineScope {
			// Given
			val transformer1 = TestTransformer1(testDispatcher)
			val transformer2 = TestTransformer2(testDispatcher)
			val transformer3 = TestTransformer3(testDispatcher)
			sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
			// When
			val effects  = sut.effectStream.testIn(backgroundScope)
			sut.processSignal(TestSignal)
			assertEquals(6, effects.cancelAndConsumeRemainingEvents().size)
			// Then
		}
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all transformers should send the correct TestData`() = runTest {
		turbineScope {
			// Given
			val transformer1 = TestTransformer1(testDispatcher)
			val transformer2 = TestTransformer2(testDispatcher)
			val transformer3 = TestTransformer3(testDispatcher)
			sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
			// When
			sut.processSignal(TestSignal)
			sut.dataStream.test {
				assertEquals(TestData("update with TestTransformer1"), awaitItem())
				assertEquals(TestData("update with TestTransformer2"), awaitItem())
				assertEquals(TestData("update with TestTransformer3"), awaitItem())
			}

			// Then
		}
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all transformers should not contain the RouterPayloadEffect`() {
		// Given
		val transformer1 = TestTransformer1(testDispatcher)
		val transformer2 = TestTransformer2(testDispatcher)
		val transformer3 = TestTransformer3(testDispatcher)
		sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(transformer1.effectList.contains(RouterEffect("")),false )
		assertEquals(transformer2.effectList.contains(RouterEffect("")),false )
		assertEquals(transformer3.effectList.contains(RouterEffect("")),false )
	}
}

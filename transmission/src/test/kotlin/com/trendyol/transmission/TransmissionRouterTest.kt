package com.trendyol.transmission

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
			sut = TransmissionRouter(setOf())
			// When
			try {
				sut.initialize(onData = {}, onEffect = {})
			} catch (e: IllegalStateException) {
				// Then
				assertEquals(e.message, "transformerSet should not be empty")
			}
		}

	@Test
	fun `GIVEN Router with one transformer, WHEN initialize is called, THEN router should not throw IllegalStateException`() =
		runTest {
			// Given
			sut = TransmissionRouter(setOf(FakeTransformer(testDispatcher)))
			// When
			val exception = try {
				sut.initialize(onData = {}, onEffect = {})
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
		sut.initialize(onEffect = {}, onData = {})
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
		sut.initialize(onEffect = {}, onData = {})
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
		sut.initialize(onEffect = {}, onData = {})
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(transformer1.effectList.last(), TestEffect)
		assertEquals(transformer2.effectList.last(), TestEffect)
		assertEquals(transformer3.effectList.last(), TestEffect)
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all effects should be sent through onEffect`() {
		// Given
		val transformer1 = TestTransformer1(testDispatcher)
		val transformer2 = TestTransformer2(testDispatcher)
		val transformer3 = TestTransformer3(testDispatcher)
		sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
		val effectList = mutableListOf<Transmission.Effect>()
		sut.initialize(onEffect = { effectList.add(it) }, onData = {})
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(effectList.size, 3)
	}

	@Test
	fun `GIVEN initialized Router with multiple Transformers, WHEN processSignal is called, THEN all transformers should send the correct TestData`() {
		// Given
		val transformer1 = TestTransformer1(testDispatcher)
		val transformer2 = TestTransformer2(testDispatcher)
		val transformer3 = TestTransformer3(testDispatcher)
		val dataList = mutableListOf<Transmission.Data>()
		sut = TransmissionRouter(setOf(transformer1, transformer2, transformer3), testDispatcher)
		sut.initialize(onEffect = {}, onData = { dataList.add(it) })
		// When
		sut.processSignal(TestSignal)

		// Then
		assertEquals(dataList.size, 3)
		assertEquals(TestData("update with TestTransformer1"), dataList[0])
		assertEquals(TestData("update with TestTransformer2"), dataList[1])
		assertEquals(TestData("update with TestTransformer3"), dataList[2])
	}
}

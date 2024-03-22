package com.trendyol.transmission

import com.trendyol.transmission.transformer.FakeTransformer
import com.trendyol.transmission.transformer.TestTransformer1
import com.trendyol.transmission.transformer.TestTransformer2
import com.trendyol.transmission.transformer.TestTransformer3
import com.trendyol.transmission.transformer.data.TestSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import kotlin.test.assertEquals

class TransmissionRouterTest {

	private lateinit var sut: TransmissionRouter

	@OptIn(ExperimentalCoroutinesApi::class)
	@BeforeEach
	fun beforeEachTest() {
		Dispatchers.setMain(StandardTestDispatcher())
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@AfterEach
	fun afterEachTest() {
		Dispatchers.resetMain()
	}

	@Test
	fun `GIVEN Router with no transformers, WHEN initialize is called, THEN router should throw IllegalStateException`() = runTest {
		// Given
		sut = TransmissionRouter(setOf())
		// When
		try {
			sut.initialize(onData = {}, onEffect = {})
		} catch(e: IllegalStateException) {
			// Then
			assertEquals(e.message, "transformerSet should not be empty")
		}
	}

	@Test
	fun `GIVEN Router with one transformer, WHEN initialize is called, THEN router should not throw IllegalStateException`() = runTest {
		// Given
		sut = TransmissionRouter(setOf(FakeTransformer()))
		// When
		val exception = try {
			sut.initialize(onData = {}, onEffect = {})
			null
		} catch(e: IllegalStateException) {
			e
		}
		// Then
		assertEquals(exception, null)
	}
}

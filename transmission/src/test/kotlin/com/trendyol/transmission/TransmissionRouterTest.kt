package com.trendyol.transmission

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
		Dispatchers.setMain(UnconfinedTestDispatcher())
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
}

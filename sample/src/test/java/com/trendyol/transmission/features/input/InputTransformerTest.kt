package com.trendyol.transmission.features.input

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.InputUiState
import com.yigitozgumus.transmissiontesting.testWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InputTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sut: InputTransformer


    @Before
    fun setUp() {
        sut = InputTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN inputTransformer, WHEN inputUpdate signal is sent, THEN inputUpdate effect is published`() =
        runTest {
            turbineScope {
                sut.testWith {
                    sendSignal(InputSignal.InputUpdate("test"))
//                    effectStream.test {
//                        assertEquals(InputEffect.InputUpdate("test"),awaitItem())
//                    }
                    dataStream.test {
                        assertEquals(InputUiState("test"), expectMostRecentItem())
                    }
                }
            }
        }
}
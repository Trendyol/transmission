package com.trendyol.transmission.features.input

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
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
            sut.testWith(InputSignal.InputUpdate("test")) {
                effectStream.test {
                    assertEquals(InputEffect.InputUpdate("test"), awaitItem())
                }
                dataStream.test {
                    assertEquals(InputUiState("test"), expectMostRecentItem())
                }
            }
        }

    @Test
    fun `GIVEN inputTransformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() = runTest {
        sut.testWith(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
            dataStream.test {
                assertEquals(InputUiState(backgroundColor = Color.Gray), expectMostRecentItem())
            }
        }
    }
}

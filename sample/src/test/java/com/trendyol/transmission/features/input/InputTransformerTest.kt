package com.trendyol.transmission.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.InputUiState
import com.yigitozgumus.transmissiontesting.testWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InputTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var sut: InputTransformer

    @Before
    fun setUp() {
        sut = InputTransformer(UnconfinedTestDispatcher())
    }

    @Test
    fun `GIVEN inputTransformer, WHEN inputUpdate signal is sent, THEN inputUpdate effect is published`() {
        sut.testWith {
            sendSignal(InputSignal.InputUpdate("test"))
            assertEquals(InputEffect.InputUpdate("test"), effectStream.first().effect)
            assertEquals(InputUiState("test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() {
        sut.testWith {
            sendEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray))
            assertEquals(InputUiState(backgroundColor = Color.Gray), dataStream.last())
        }
    }
}

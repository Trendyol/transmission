package com.trendyol.transmission.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.InputUiState
import com.yigitozgumus.transmissiontesting.testWithEffect
import com.yigitozgumus.transmissiontesting.testWithSignal
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class InputTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var sut: InputTransformer

    @Before
    fun setUp() {
        sut = InputTransformer(testCoroutineRule.testDispatcher)
    }

    @Test
    fun `GIVEN inputTransformer, WHEN inputUpdate signal is sent, THEN inputUpdate effect is published`() {
        sut.testWithSignal(signal = InputSignal.InputUpdate("test")) {
            assertEquals(InputEffect.InputUpdate("test"), effectStream.first().effect)
            assertEquals(InputUiState("test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() {
        sut.testWithEffect(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
            assertEquals(InputUiState(backgroundColor = Color.Gray), dataStream.last())
        }
    }
}

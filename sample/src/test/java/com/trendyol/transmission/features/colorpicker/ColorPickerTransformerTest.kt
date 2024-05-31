package com.trendyol.transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.yigitozgumus.transmissiontesting.testTransformer
import com.yigitozgumus.transmissiontesting.testWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorPickerTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var sut: ColorPickerTransformer

    @Before
    fun setUp() {
        sut = ColorPickerTransformer(testCoroutineRule.testDispatcher)
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed 1`() {
        testTransformer(transformerProvider = { ColorPickerTransformer(this) }) {
            sendEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray))
            assertEquals(
                ColorPickerUiState(backgroundColor = Color.Gray),
                dataStream.last()
            )
        }
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() =
        sut.testWith {
            sendEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray))
            assertEquals(
                ColorPickerUiState(backgroundColor = Color.Gray),
                dataStream.last()
            )
        }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN selectedColorIndex should be updated`() =
        sut.testWith {
            sendSignal(ColorPickerSignal.SelectColor(3, Color.Blue))
            assertEquals(
                3,
                (dataStream.last() as ColorPickerUiState).selectedColorIndex
            )
        }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN BackgroundColorUpdate effect should be published`() {
        sut.testWith {
            sendSignal(ColorPickerSignal.SelectColor(3, Color.Blue))
            assertEquals(
                Color.Blue.copy(alpha = 0.1f),
                (effectStream.first().effect as ColorPickerEffect.BackgroundColorUpdate).color
            )
        }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN SelectedColorUpdate is sent to MultiOutputTransformer`() {
        // Given
        sut.testWith {
            // When
            sendSignal(ColorPickerSignal.SelectColor(3, Color.Blue))
            // Then
            assertTrue { effectStream.last().effect is ColorPickerEffect.SelectedColorUpdate }
        }
    }

}

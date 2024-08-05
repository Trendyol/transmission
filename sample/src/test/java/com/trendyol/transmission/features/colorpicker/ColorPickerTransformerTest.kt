package com.trendyol.transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmissiontest.attachToRouter
import com.trendyol.transmissiontest.test
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
        sut.attachToRouter()
            .test(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    dataStream.last()
                )
            }
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() =
        sut.attachToRouter()
            .test(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    dataStream.last()
                )
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN selectedColorIndex should be updated`() =
        sut.attachToRouter()
            .test(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                    3,
                    (dataStream.last() as ColorPickerUiState).selectedColorIndex
                )
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN BackgroundColorUpdate effect should be published`() {
        sut.attachToRouter()
            .test(signal = ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                    Color.Blue.copy(alpha = 0.1f),
                    (effectStream.first() as ColorPickerEffect.BackgroundColorUpdate).color
                )
            }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN SelectedColorUpdate is sent to MultiOutputTransformer`() {
        sut.attachToRouter().test(signal = ColorPickerSignal.SelectColor(3, Color.Blue)) {
            assertTrue { effectStream.last() is ColorPickerEffect.SelectedColorUpdate }
        }
    }
}

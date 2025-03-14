package com.trendyol.transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.features.colorpicker.ColorPickerSignal
import com.trendyol.transmission.components.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.transformer.util.TestCoroutineRule
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
        sut.test()
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    lastData()
                )
            }
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() =
        sut.test()
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    lastData<ColorPickerUiState>()
                )
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN selectedColorIndex should be updated`() =
        sut.test()
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                   3,
                    lastData<ColorPickerUiState>()?.selectedColorIndex
                )
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN BackgroundColorUpdate effect should be published`() {
        sut.test()
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                    Color.Blue.copy(alpha = 0.1f),
                    allEffects<ColorPickerEffect.BackgroundColorUpdate>().first().color
                )
            }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN SelectedColorUpdate is sent to MultiOutputTransformer`() {
        sut.test()
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
               assertTrue { lastEffect<ColorPickerEffect.SelectedColorUpdate>() != null }
            }
    }
}

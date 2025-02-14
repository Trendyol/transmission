package com.trendyol.transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
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
        sut.attachToRouter()
            .test(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                turbineScope {
                    val dataCollector = dataStream.testIn(it.backgroundScope)
                    assertEquals(
                        ColorPickerUiState(backgroundColor = Color.Gray),
                        dataCollector.expectMostRecentItem()
                    )
                }
            }
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() =
        sut.attachToRouter()
            .test(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                turbineScope {
                    val dataCollector = dataStream.testIn(it.backgroundScope)
                    assertEquals(
                        ColorPickerUiState(backgroundColor = Color.Gray),
                        dataCollector.expectMostRecentItem()
                    )
                }
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN selectedColorIndex should be updated`() =
        sut.attachToRouter()
            .test(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                turbineScope {
                    val dataCollector = dataStream.testIn(it.backgroundScope)
                    assertEquals(
                        3,
                        (dataCollector.expectMostRecentItem() as ColorPickerUiState).selectedColorIndex
                    )
                }
            }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN BackgroundColorUpdate effect should be published`() {
        sut.attachToRouter()
            .test(signal = ColorPickerSignal.SelectColor(3, Color.Blue)) {
                turbineScope {
                    val effectCollector = effectStream.testIn(it.backgroundScope)
                    assertEquals(
                        Color.Blue.copy(alpha = 0.1f),
                        (effectCollector.awaitItem() as ColorPickerEffect.BackgroundColorUpdate).color
                    )
                }
            }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN SelectedColorUpdate is sent to MultiOutputTransformer`() {
        sut.attachToRouter()
            .test(signal = ColorPickerSignal.SelectColor(3, Color.Blue)) {
                turbineScope {
                    val effectCollector = effectStream.testIn(it.backgroundScope)
                    assertTrue { effectCollector.expectMostRecentItem() is ColorPickerEffect.SelectedColorUpdate }
                }
            }
    }
}

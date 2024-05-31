package com.trendyol.transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import app.cash.turbine.test
import com.trendyol.transmission.features.multioutput.MultiOutputTransformer
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.yigitozgumus.transmissiontesting.testWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ColorPickerTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sut: ColorPickerTransformer

    @Before
    fun setUp() {
        sut = ColorPickerTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN transformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() =
        runTest {
            sut.testWith(ColorPickerEffect.BackgroundColorUpdate(Color.Gray), testDispatcher) {
                dataStream.test {
                    assertEquals(
                        ColorPickerUiState(backgroundColor = Color.Gray),
                        expectMostRecentItem()
                    )
                }
            }
        }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN selectedColorIndex should be updated`() =
        runTest {
            sut.testWith(ColorPickerSignal.SelectColor(3, Color.Blue), testDispatcher) {
                dataStream.test {
                    assertEquals(
                        3,
                        (expectMostRecentItem() as ColorPickerUiState).selectedColorIndex
                    )
                }
            }
        }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN BackgroundColorUpdate effect should be published`() =
        runTest {
            sut.testWith(ColorPickerSignal.SelectColor(3, Color.Blue), testDispatcher) {
                effectStream.test {
                    assertEquals(
                        Color.Blue.copy(alpha = 0.1f),
                        (awaitItem().effect as ColorPickerEffect.BackgroundColorUpdate).color
                    )
                    awaitItem()
                }
            }
        }

    @Test
    fun `GIVEN inputTransformer, WHEN SelectColor signal is sent, THEN SelectedColorUpdate is sent to MultiOutputTransformer`() =
        runTest {
            sut.testWith(ColorPickerSignal.SelectColor(3, Color.Blue), testDispatcher) {
                effectStream.test {
                    val recent = expectMostRecentItem()
                    assertEquals(
                        Color.Blue,
                        (recent.effect as ColorPickerEffect.SelectedColorUpdate).color
                    )
                    assertEquals(
                        MultiOutputTransformer::class,
                        recent.receiver
                    )
                }
            }
        }
}

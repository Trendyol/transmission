package transmission.features.colorpicker

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.components.ColorPickerUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.colorpicker.ColorPickerSignal
import com.trendyol.transmission.components.colorpicker.ColorPickerTransformer
import com.trendyol.transmissiontest.test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorPickerTransformerTest {

    private lateinit var sut: ColorPickerTransformer

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        sut = ColorPickerTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN transformer WHEN BackgroundColorUpdate effect is received THEN color should be changed 1`() {
        sut.test(testDispatcher)
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    lastData()
                )
            }
    }

    @Test
    fun `GIVEN transformer WHEN BackgroundColorUpdate effect is received THEN color should be changed`() =
        sut.test(testDispatcher)
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(
                    ColorPickerUiState(backgroundColor = Color.Gray),
                    lastData<ColorPickerUiState>()
                )
            }

    @Test
    fun `GIVEN inputTransformer WHEN SelectColor signal is sent THEN selectedColorIndex should be updated`() =
        sut.test(testDispatcher)
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                    3,
                    lastData<ColorPickerUiState>()?.selectedColorIndex
                )
            }

    @Test
    fun `GIVEN inputTransformer WHEN SelectColor signal is sent THEN BackgroundColorUpdate effect should be published`() {
        sut.test(testDispatcher)
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertEquals(
                    Color.Blue.copy(alpha = 0.1f),
                    allEffects<ColorPickerEffect.BackgroundColorUpdate>().first().color
                )
            }
    }

    @Test
    fun `GIVEN inputTransformer WHEN SelectColor signal is sent THEN SelectedColorUpdate is sent to MultiOutputTransformer`() {
        sut.test(testDispatcher)
            .testSignal(ColorPickerSignal.SelectColor(3, Color.Blue)) {
                assertTrue { lastEffect<ColorPickerEffect.SelectedColorUpdate>() != null }
            }
    }
}

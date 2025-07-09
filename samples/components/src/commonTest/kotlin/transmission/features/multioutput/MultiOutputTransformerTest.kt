package transmission.features.multioutput

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.components.MultiOutputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.multioutput.MultiOutputTransformer
import com.trendyol.transmission.components.output.OutputCalculationResult
import com.trendyol.transmission.components.output.OutputTransformer
import com.trendyol.transmissiontest.test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiOutputTransformerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var sut: MultiOutputTransformer

    @BeforeTest
    fun setUp() {
        sut = MultiOutputTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN InputUpdate effect WHEN processed THEN writtenUppercaseText is updated and output is appended`() {
        sut.test(testDispatcher)
            .computation(OutputTransformer.outputCalculationContract) {
                OutputCalculationResult(result = "42")
            }
            .testEffect(InputEffect.InputUpdate("hello")) {
                assertEquals(
                    MultiOutputUiState(writtenUppercaseText = "HELLO 42"),
                    lastData<MultiOutputUiState>()
                )
            }
    }

    @Test
    fun `GIVEN BackgroundColorUpdate effect WHEN processed THEN backgroundColor is updated`() {
        sut.test(testDispatcher)
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(color = Color.Blue)) {
                assertEquals(Color.Blue, lastData<MultiOutputUiState>()?.backgroundColor)
            }
    }

    @Test
    fun `GIVEN SelectedColorUpdate effect WHEN processed THEN selectedColor is updated`() {
        sut.test(testDispatcher)
            .testEffect(ColorPickerEffect.SelectedColorUpdate(color = Color.Red)) {
                assertEquals(Color.Red, lastData<MultiOutputUiState>()?.selectedColor)
            }
    }
} 
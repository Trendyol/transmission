package transmission.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.InputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.input.InputSignal
import com.trendyol.transmission.components.input.InputTransformer
import com.trendyol.transmissiontest.test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InputTransformerTest {

    private lateinit var sut: InputTransformer

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        sut = InputTransformer(testDispatcher)
    }

    @OptIn(ExperimentalTransmissionApi::class)
    @Test
    fun `GIVEN inputTransformer WHEN inputUpdate signal is sent THEN inputUpdate effect is published`() {
        sut.test(testDispatcher)
            .checkpointWithArgs(InputTransformer.colorCheckpoint, Color.Gray)
            .testSignal(InputSignal.InputUpdate("test")) {
               assertEquals(InputEffect.InputUpdate("test"), lastEffect())
                assertEquals(InputUiState("test"), lastData())
            }
    }

    @Test
    fun `GIVEN inputTransformer WHEN BackgroundColorUpdate effect is received THEN color should be changed`() {
        sut.test(testDispatcher)
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(InputUiState(backgroundColor = Color.Gray), lastData())
            }
    }
}

package transmission.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.InputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.input.InputSignal
import com.trendyol.transmission.components.input.InputTransformer
import com.trendyol.transmissiontest.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InputTransformerTest {

    private lateinit var sut: InputTransformer

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher) // Set test dispatcher as main
        sut = InputTransformer(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain() // Reset main dispatcher
    }

    @OptIn(ExperimentalTransmissionApi::class)
    @Test
    fun `GIVEN inputTransformer WHEN inputUpdate signal is sent THEN inputUpdate effect is published`() {
        sut.test()
            .checkpointWithArgs(InputTransformer.colorCheckpoint, Color.Gray)
            .testSignal(InputSignal.InputUpdate("test")) {
                assertEquals(InputEffect.InputUpdate("test"), lastEffect())
                assertEquals(InputUiState("test"), lastData())
            }
    }

    @Test
    fun `GIVEN inputTransformer WHEN BackgroundColorUpdate effect is received THEN color should be changed`() {
        sut.test()
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(InputUiState(backgroundColor = Color.Gray), lastData())
            }
    }
}

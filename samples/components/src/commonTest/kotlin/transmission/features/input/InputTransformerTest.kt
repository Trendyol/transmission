package transmission.features.input

import androidx.compose.ui.graphics.Color
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.InputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerEffect
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.input.InputSignal
import com.trendyol.transmission.components.input.InputTransformer
import transmission.util.TestCoroutineRule
import com.trendyol.transmissiontest.test
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

    @OptIn(ExperimentalTransmissionApi::class)
    @Test
    fun `GIVEN inputTransformer, WHEN inputUpdate signal is sent, THEN inputUpdate effect is published`() {
        sut.test()
            .withCheckpoint(InputTransformer.colorCheckpoint, Color.Gray)
            .testSignal(InputSignal.InputUpdate("test")) {
                assertEquals(InputEffect.InputUpdate("test"), lastEffect())
                assertEquals(InputUiState("test"), lastData())
            }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() {
        sut.test()
            .testEffect(ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                assertEquals(InputUiState(backgroundColor = Color.Gray), lastData())
            }
    }
}

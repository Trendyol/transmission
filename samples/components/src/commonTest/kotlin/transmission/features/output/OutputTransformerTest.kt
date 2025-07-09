package transmission.features.output

import com.trendyol.transmission.components.ColorPickerUiState
import com.trendyol.transmission.components.OutputUiState
import com.trendyol.transmission.components.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.components.input.InputEffect
import com.trendyol.transmission.components.output.OutputTransformer
import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmissiontest.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutputTransformerTest {

    private lateinit var sut: OutputTransformer

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sut = OutputTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN sut WHEN inputUpdate effect comes THEN holder should be updated with correct value`() {
        sut.test(testDispatcher)
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), lastData<OutputUiState>())
            }
    }

    @Test
    fun `GIVEN sut WHEN inputUpdate effect comes and no ColorPickerUIState data is present THEN data should not be updated further`() {
        sut.test(testDispatcher)
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), lastData<OutputUiState>())
            }
    }

    @Test
    fun `GIVEN sut WHEN inputUpdate effect comes and ColorPickerUIState exists THEN RouterPayloadEffect should be published`() {
        sut.test(testDispatcher)
            .dataHolder(ColorPickerTransformer.holderContract) {
                ColorPickerUiState()
            }
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), nthData(1))
                assertTrue(effectStream.filterIsInstance<RouterEffect>().isNotEmpty())
            }
    }
}

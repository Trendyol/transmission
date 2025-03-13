package com.trendyol.transmission.features.output

import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.components.features.input.InputEffect
import com.trendyol.transmission.components.features.output.OutputTransformer
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.components.features.ColorPickerUiState
import com.trendyol.transmission.components.features.OutputUiState
import com.trendyol.transmission.components.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmissiontest.attachToRouter
import com.trendyol.transmissiontest.test
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutputTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private lateinit var sut: OutputTransformer

    @Before
    fun setUp() {
        sut = OutputTransformer(testCoroutineRule.testDispatcher)
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes, THEN, holder should be updated with correct value`() {
        sut.test()
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), lastData<OutputUiState>())
            }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and no ColorPickerUIState data is present, THEN data should not be updated further`() {
        sut.test()
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), lastData<OutputUiState>())
            }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published`() {
        sut.test()
            .withData(ColorPickerTransformer.holderContract) {
                ColorPickerUiState()
            }
            .testEffect(InputEffect.InputUpdate("test")) {
                assertEquals(OutputUiState(outputText = "test"), nthData(1))
                assertTrue(lastEffect() is RouterEffect )
            }
    }
}

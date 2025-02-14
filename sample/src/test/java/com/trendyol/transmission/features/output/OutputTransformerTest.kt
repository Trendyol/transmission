package com.trendyol.transmission.features.output

import app.cash.turbine.testIn
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
        sut.attachToRouter()
            .test(effect = InputEffect.InputUpdate("test")) {
                val dataCollector = dataStream.testIn(it.backgroundScope)
                assertEquals(OutputUiState(outputText = "test"), dataCollector.expectMostRecentItem())
            }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and no ColorPickerUIState data is present, THEN data should not be updated further`() {
        sut.attachToRouter()
            .test(effect = InputEffect.InputUpdate("test")) {
                val dataCollector = dataStream.testIn(it.backgroundScope)
                assertEquals(OutputUiState(outputText = "test"), dataCollector.expectMostRecentItem())
            }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published`() {
        sut.attachToRouter()
            .registerData(ColorPickerTransformer.holderContract) {
                ColorPickerUiState()
            }
            .test(effect = InputEffect.InputUpdate("test")) {
                val dataCollector = dataStream.testIn(it.backgroundScope)
                val effectCollector = effectStream.testIn(it.backgroundScope)
                dataCollector.skipItems(1)
                assertEquals(OutputUiState(outputText = "test"), dataCollector.awaitItem())
                assertTrue(effectCollector.expectMostRecentItem() is RouterEffect)
            }
    }
}

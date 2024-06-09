package com.trendyol.transmission.features.output

import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import com.yigitozgumus.transmissiontesting.testWithEffect
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
        sut.testWithEffect(effect = InputEffect.InputUpdate("test")) {
            assertEquals(OutputUiState(outputText = "test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and no ColorPickerUIState data is present, THEN data should not be updated further`() {
        sut.testWithEffect(effect = InputEffect.InputUpdate("test")) {
            assertEquals(OutputUiState(outputText = "test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published`() {
        sut.testWithEffect(effect = InputEffect.InputUpdate("test"), registry = {
            addQueryData(ColorPickerUiState(), key = "ColorPickerUiState")
        }) {
            assertEquals(OutputUiState(outputText = "test"), dataStream[1])
            assertTrue(effectStream.last().effect is RouterEffect)
        }
    }
}

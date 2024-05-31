package com.trendyol.transmission.features.output

import com.trendyol.transmission.effect.RouterEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformerTest
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import com.yigitozgumus.transmissiontesting.testTransformer
import com.yigitozgumus.transmissiontesting.testWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
        sut.testWith {
            sendEffect(InputEffect.InputUpdate("test"))
            assertEquals(OutputUiState(outputText = "test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and no ColorPickerUIState data is present, THEN data should not be updated further`() {
        sut.testWith {
            sendEffect(InputEffect.InputUpdate("test"))
            assertEquals(OutputUiState(outputText = "test"), dataStream.last())
        }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published`() {
        sut.testWith(registry = {
            addQueryData(ColorPickerUiState(), ColorPickerTransformer::class)
        }) {
            sendEffect(InputEffect.InputUpdate("test"))
//            assertEquals(OutputUiState(outputText = "test"), dataStream[1])
            assertTrue(effectStream.last().effect is RouterEffect)
        }
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published 1`() {
        testTransformer(transformerProvider = {OutputTransformer(this)}, registry = {
            addQueryData(ColorPickerUiState(), ColorPickerTransformer::class)
        }) {
            sendEffect(InputEffect.InputUpdate("test"))
//            assertEquals(OutputUiState(outputText = "test"), dataStream[1])
            assertTrue(effectStream.last().effect is RouterEffect)
        }
    }
}

package com.trendyol.transmission.features.input

import androidx.compose.ui.graphics.Color
import app.cash.turbine.testIn
import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.components.features.input.InputEffect
import com.trendyol.transmission.components.features.input.InputSignal
import com.trendyol.transmission.components.features.colorpicker.ColorPickerEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.components.features.InputUiState
import com.trendyol.transmission.components.features.input.InputTransformer
import com.trendyol.transmissiontest.attachToRouter
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
        sut.attachToRouter()
            .registerCheckpoint(InputTransformer.colorCheckpoint, Color.Gray)
            .test(signal = InputSignal.InputUpdate("test")) {
                val effectCollector = effectStream.testIn(it.backgroundScope)
                val dataCollector = dataStream.testIn(it.backgroundScope)
                assertEquals(InputEffect.InputUpdate("test"), effectCollector.expectMostRecentItem())
                assertEquals(InputUiState("test"), dataCollector.expectMostRecentItem())
            }
    }

    @Test
    fun `GIVEN inputTransformer, WHEN BackgroundColorUpdate effect is received, THEN color should be changed`() {
        sut.attachToRouter()
            .test(effect = ColorPickerEffect.BackgroundColorUpdate(Color.Gray)) {
                val dataCollector = dataStream.testIn(it.backgroundScope)
                assertEquals(InputUiState(backgroundColor = Color.Gray), dataCollector.expectMostRecentItem())
            }
    }
}

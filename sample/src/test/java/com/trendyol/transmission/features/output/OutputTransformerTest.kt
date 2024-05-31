package com.trendyol.transmission.features.output

import app.cash.turbine.test
import com.trendyol.transmission.effect.RouterPayloadEffect
import com.trendyol.transmission.features.colorpicker.ColorPickerTransformer
import com.trendyol.transmission.features.input.InputEffect
import com.trendyol.transmission.transformer.util.TestCoroutineRule
import com.trendyol.transmission.ui.ColorPickerUiState
import com.trendyol.transmission.ui.OutputUiState
import com.yigitozgumus.transmissiontesting.testWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OutputTransformerTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var sut: OutputTransformer

    @Before
    fun setUp() {
        sut = OutputTransformer(testDispatcher)
    }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes, THEN, holder should be updated with correct value`() =
        runTest {
            sut.testWith(InputEffect.InputUpdate("test"), testDispatcher) {
                dataStream.test {
                    assertEquals(OutputUiState(outputText = "test"), expectMostRecentItem())
                }
            }
        }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and no ColorPickerUIState data is present, THEN data should not be updated further`() =
        runTest {
            sut.testWith(InputEffect.InputUpdate("test"), testDispatcher) {
                dataStream.test {
                    assertEquals(OutputUiState(outputText = "test"), expectMostRecentItem())
                    expectNoEvents()
                }
                effectStream.test {
                    expectNoEvents()
                }
            }
        }

    @Test
    fun `GIVEN sut, WHEN inputUpdate effect comes and ColorPickerUIState exists, THEN RouterPayloadEffect should be published`() =
        runTest {
            sut.testWith(InputEffect.InputUpdate("test"),testDispatcher, registry = {
                addQueryData(ColorPickerUiState(), ColorPickerTransformer::class)
            }) {
                dataStream.test {
                    awaitItem()
                    assertEquals(
                        OutputUiState(outputText = "test"),
                        awaitItem()
                    )
                    awaitItem()
                }
                effectStream.test {
                    assertTrue(awaitItem().effect is RouterPayloadEffect)
                    awaitItem()
                }
            }
        }
}

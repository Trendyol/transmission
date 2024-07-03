package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.test.TestScope

fun Transformer.attachToRouter(): TestSuite {
    return TestSuite().initialize(this)
}

fun TestSuite.test(
    effect: Transmission.Effect,
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    this.runTest(effect, scope)
}

fun TestSuite.test(
    signal: Transmission.Signal,
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    this.runTest(signal, scope)
}

suspend fun Transformer.waitProcessingToFinish() {
    currentSignalProcessing?.join()
    currentEffectProcessing?.join()
}

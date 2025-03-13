package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.test.TestScope

@Deprecated(
    message = "Use test() extension function instead",
    replaceWith = ReplaceWith("test()", "com.trendyol.transmissiontest.TransmissionTest")
)
fun Transformer.attachToRouter(): TestSuite {
    return TestSuite().initialize(this)
}

@Deprecated(
    message = "Use testEffect() instead",
    replaceWith =
        ReplaceWith(
            "test().testEffect(effect, scope)",
            "com.trendyol.transmissiontest.TransmissionTest"
        )
)
fun TestSuite.test(
    effect: Transmission.Effect,
    scope: suspend TransformerTestScope.(scope: TestScope) -> Unit
) {
    this.runTest(effect, scope)
}

@Deprecated(
    message = "Use testSignal() instead",
    replaceWith =
        ReplaceWith(
            "test().testSignal(signal, scope)",
            "com.trendyol.transmissiontest.TransmissionTest"
        )
)
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

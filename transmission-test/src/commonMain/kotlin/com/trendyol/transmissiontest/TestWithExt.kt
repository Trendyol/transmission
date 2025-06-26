package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.test.TestScope

/**
 * @deprecated This method is deprecated. Use [Transformer.test] extension function instead.
 * 
 * Creates a [TestSuite] for the transformer. The TestSuite API is deprecated in favor of
 * the more modern [TransmissionTest] API which provides better type safety and readability.
 * 
 * @receiver The transformer to test
 * @return A TestSuite instance
 */
@Deprecated(
    message = "Use test() extension function instead",
    replaceWith = ReplaceWith("test()", "com.trendyol.transmissiontest.TransmissionTest")
)
fun Transformer.attachToRouter(): TestSuite {
    return TestSuite().initialize(this)
}

/**
 * @deprecated This method is deprecated. Use [TransmissionTest.testEffect] instead.
 * 
 * Executes a test with an effect transmission using the legacy TestSuite API.
 * 
 * @receiver The TestSuite instance
 * @param effect The effect transmission to test
 * @param scope Lambda with test assertions
 */
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

/**
 * @deprecated This method is deprecated. Use [TransmissionTest.testSignal] instead.
 * 
 * Executes a test with a signal transmission using the legacy TestSuite API.
 * 
 * @receiver The TestSuite instance
 * @param signal The signal transmission to test
 * @param scope Lambda with test assertions
 */
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

/**
 * Waits for the transformer's current signal and effect processing to complete.
 * 
 * This function suspends until both the current signal processing and effect processing
 * coroutines have finished executing. This is useful in testing scenarios where you need
 * to ensure all transformer operations have completed before making assertions.
 * 
 * @receiver The transformer whose processing should be waited for
 * 
 * Example usage:
 * ```kotlin
 * // Process a signal
 * router.process(UserSignal.Login("user", "pass"))
 * 
 * // Wait for transformer to finish processing
 * transformer.waitProcessingToFinish()
 * 
 * // Now safe to make assertions
 * val userData = queryHandler.getData(UserTransformer.dataContract)
 * assertNotNull(userData)
 * ```
 */
suspend fun Transformer.waitProcessingToFinish() {
    currentSignalProcessing?.join()
    currentEffectProcessing?.join()
}

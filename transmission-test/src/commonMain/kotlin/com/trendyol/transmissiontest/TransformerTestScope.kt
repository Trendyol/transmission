package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission

/**
 * Test scope interface for the legacy TestSuite API.
 *
 * @deprecated This interface is part of the deprecated TestSuite API. Use [TestResult] from
 * [TransmissionTest] instead, which provides more comprehensive assertion methods.
 *
 * This interface provides access to captured data and effect streams during test execution.
 * It was used with the legacy TestSuite API but has been superseded by the more powerful
 * TestResult class in the modern TransmissionTest framework.
 *
 * @property dataStream List of all data transmissions captured during test execution
 * @property effectStream List of all effect transmissions captured during test execution
 *
 * @see TestResult for the modern replacement
 * @see TransmissionTest for the recommended testing approach
 */
@Deprecated(
    message = "Use TestResult from TransmissionTest instead",

    )
interface TransformerTestScope {
    /**
     * List of all data transmissions captured during test execution.
     *
     * @deprecated Use [TestResult.dataStream] instead
     */
    val dataStream: List<Transmission.Data>

    /**
     * List of all effect transmissions captured during test execution.
     *
     * @deprecated Use [TestResult.effectStream] instead
     */
    val effectStream: List<Transmission.Effect>
}

package com.trendyol.transmission

/**
 * Marks internal declarations in Transmission. Internal declarations must not be used outside the library.
 * There are no backward compatibility guarantees between different versions of Transmission.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
annotation class InternalTransmissionApi

/**
 * Marks experimental API in Transmission. An experimental API can be changed or removed at any time.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalTransmissionApi

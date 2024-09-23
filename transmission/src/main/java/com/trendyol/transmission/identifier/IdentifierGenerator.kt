package com.trendyol.transmission.identifier

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Generates identifier for internal communication.
 */
@OptIn(ExperimentalUuidApi::class)
internal object IdentifierGenerator {
    fun generateIdentifier(): String {
        return Uuid.random().toString()
    }
}

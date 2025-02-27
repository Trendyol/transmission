package com.trendyol.transmission.config

/**
 * Configuration class for Transformers
 */
data class TransformerConfig private constructor(
    /**
     * Whether to enable type hierarchy awareness for signal and effect handlers
     * When enabled, handlers registered for a supertype will be invoked
     * for all instances of subtypes as well.
     *
     * Default: false (for backward compatibility)
     */
    val typeHierarchyAwarenessEnabled: Boolean = false
) {

    companion object {
        /**
         * Default configuration
         */
        val Default = TransformerConfig()

        /**
         * Type Aware configuration
         */
        val TypeAware = TransformerConfig(typeHierarchyAwarenessEnabled = true)
    }
}
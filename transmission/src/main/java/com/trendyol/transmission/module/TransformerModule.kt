package com.trendyol.transmission.module

import com.trendyol.transmission.transformer.handler.HandlerScope
import com.trendyol.transmission.transformer.request.computation.ComputationScope
import com.trendyol.transmission.transformer.request.execution.ExecutionScope

/**
 * Interface for defining reusable modules that can be applied to Transformers
 */
interface TransformerModule {
    /**
     * Configure handlers for a transformer
     * @param scope The handler scope to configure
     */
    fun configureHandlers(scope: HandlerScope) {}

    /**
     * Called when the transformer is cleared
     */
    fun onCleared() {}

    /**
     * Called when an error occurs in the transformer
     */
    fun onError(throwable: Throwable) {}
}
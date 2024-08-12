package com.trendyol.transmission.router.loader

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.router.TransmissionRouter

interface TransformerSetLoader {
    /**
     * This method loads the transformer set to the [TransmissionRouter]
     */
    suspend fun load(): Set<Transformer>
}

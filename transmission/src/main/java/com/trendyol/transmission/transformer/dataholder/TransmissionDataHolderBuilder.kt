package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer

class TransmissionDataHolderBuilder<T : Transmission.Data?> {
    fun buildWith(
        initialValue: T,
        publishUpdates: Boolean = true,
        transformer: Transformer,
        holderKey: String?,
    ): TransmissionDataHolder<T> {
        return TransmissionDataHolderImpl(
            initialValue = initialValue,
            publishUpdates = publishUpdates,
            transformer = transformer,
            holderKey = holderKey,
        )
    }
}

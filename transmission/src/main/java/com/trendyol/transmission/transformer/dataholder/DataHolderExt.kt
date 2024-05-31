package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer

/**
 * Throws [IllegalArgumentException] when multiple data holders with same type
 * is defined inside a [Transformer]
 * @param initialValue Initial value of the Data Holder.
 * Must be a type extended from [Transmission.Data]
 * @param [publishUpdates] Controls sending updates to the [TransmissionRouter]
 * */
inline fun <reified T : Transmission.Data?> Transformer.buildDataHolder(
    initialValue: T,
    publishUpdates: Boolean = true,
): TransmissionDataHolder<T> {
    return TransmissionDataHolderBuilder<T>().buildWith(
        initialValue = initialValue,
        publishUpdates = publishUpdates,
        transformer = this,
        typeName = T::class.java.simpleName,
    )
}

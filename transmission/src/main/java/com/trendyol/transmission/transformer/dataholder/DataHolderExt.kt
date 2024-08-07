package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.RequestHandler

/**
 * Throws [IllegalArgumentException] when multiple data holders with same type
 * is defined inside a [Transformer]
 * @param initialValue Initial value of the Data Holder.
 * Must be a type extended from [Transmission.Data]
 * @param [publishUpdates] Controls sending updates to the [TransmissionRouter]
 * @param [key] When defined, data inside the holder can be accessed by other Transformers in the
 * network using [RequestHandler.query]
 * */
fun <T : Transmission.Data?> Transformer.buildDataHolder(
    initialValue: T,
    contract: Contract.Data<T>? = null,
    publishUpdates: Boolean = true
): TransmissionDataHolder<T> {
    return TransmissionDataHolderBuilder.buildWith(
        initialValue = initialValue,
        publishUpdates = publishUpdates,
        transformer = this,
        holderKey = contract?.key,
    )
}

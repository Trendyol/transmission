package com.trendyol.transmissiontest.data

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.CoroutineDispatcher

class DataTransformer<D : Transmission.Data?>(
    contract: Contract.DataHolder<D>, data: () -> D,
    coroutineDispatcher: CoroutineDispatcher,
) : Transformer(dispatcher = coroutineDispatcher) {

    private val dataHolder = dataHolder(
        initialValue = data(),
        contract = contract,
        publishUpdates = false
    )
}

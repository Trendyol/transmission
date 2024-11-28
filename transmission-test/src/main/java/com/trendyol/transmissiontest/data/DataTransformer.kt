package com.trendyol.transmissiontest.data

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.dataholder.dataHolder
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class DataTransformer<D : Transmission.Data?>(
    contract: Contract.DataHolder<D>, data: () -> D
) : Transformer(dispatcher = UnconfinedTestDispatcher()) {

    private val dataHolder = dataHolder(
        initialValue = data(),
        contract = contract,
        publishUpdates = false
    )
}

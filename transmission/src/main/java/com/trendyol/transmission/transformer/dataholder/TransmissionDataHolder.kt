package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TransmissionDataHolder<T : Transmission.Data?> {
    fun getValue(): T
    fun update(updater: (T) -> @UnsafeVariance T)
    suspend fun updateAndGet(updater: (T) -> @UnsafeVariance T): T
}

internal class TransmissionDataHolderImpl<T : Transmission.Data?>(
    initialValue: T,
    publishUpdates: Boolean,
    transformer: Transformer,
    contract: Contract.DataHolder<T>
) : TransmissionDataHolder<T> {

    private val holder = MutableStateFlow(initialValue)

    private val lock = Mutex()

    override fun getValue(): T {
        return holder.value
    }

    override suspend fun updateAndGet(updater: (T) -> T): T {
        return lock.withLock {
            holder.updateAndGet(updater)
        }
    }

    init {
        transformer.run {
            storage.updateHolderDataReferenceToTrack(contract.key)
            transformerScope.launch {
                holder.collect {
                    it?.let { holderData ->
                        storage.updateHolderData(holderData, contract.key)
                        if (publishUpdates) {
                            transformer.dataChannel.send(it)
                        }
                    }
                }
            }
        }
    }

    override fun update(updater: (T) -> @UnsafeVariance T) {
        holder.update(updater)
    }
}

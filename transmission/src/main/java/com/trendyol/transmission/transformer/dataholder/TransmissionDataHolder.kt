package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface TransmissionDataHolder<T : Transmission.Data?> {
    fun getValue(): T
    fun update(updater: (T) -> @UnsafeVariance T)
    suspend fun updateAndReturn(updater: (T) -> @UnsafeVariance T): T
}

@PublishedApi
internal class TransmissionDataHolderImpl<T : Transmission.Data?>(
    initialValue: T,
    publishUpdates: Boolean,
    transformer: Transformer,
    holderKey: String,
) : TransmissionDataHolder<T> {

    private val holder = MutableStateFlow(initialValue)

    private val lock = Mutex()

    override fun getValue(): T {
        return holder.value
    }

    override suspend fun updateAndReturn(updater: (T) -> T): T {
        lock.withLock {
            holder.update(updater)
            return holder.value
        }
    }

    init {
        transformer.run {
            storage.updateHolderDataReferenceToTrack(holderKey)
            transformerScope.launch {
                holder.collect {
                    it?.let { holderData ->
                        storage.updateHolderData(holderData, holderKey)
                        if (publishUpdates) {
                            transformer.dataChannel.trySend(it)
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

package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface TransmissionDataHolder<T : Transmission.Data?> {
    fun getValue(): T
    fun update(updater: (T) -> @UnsafeVariance T)
}

internal class TransmissionDataHolderImpl<T : Transmission.Data?>(
    initialValue: T,
    publishUpdates: Boolean,
    transformer: Transformer,
    holderKey: String?,
) : TransmissionDataHolder<T> {

    private val holder = MutableStateFlow(initialValue)

    override fun getValue(): T {
        return holder.value
    }

    init {
        transformer.run {
            holderKey?.let {
                storage.updateHolderDataReferenceToTrack(it)
            }
            transformerScope.launch {
                holder.collect {
                    it?.let { holderData ->
                        if (holderKey != null) {
                            storage.updateHolderData(holderData)
                        }
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

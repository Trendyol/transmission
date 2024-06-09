package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.identifier
import com.trendyol.transmission.transformer.dataholder.HolderState
import com.trendyol.transmission.transformer.query.ComputationOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TransformerStorage {

    private val holderDataReference: MutableStateFlow<MutableMap<String, Transmission.Data?>> =
        MutableStateFlow(mutableMapOf())

    private var internalTransmissionHolderSet: HolderState = HolderState.Undefined

    private val internalComputationMap: MutableMap<String, ComputationOwner> =
        mutableMapOf()

    fun isHolderStateInitialized(): Boolean {
        return internalTransmissionHolderSet is HolderState.Initialized
    }

    fun isHolderDataDefined(key: String): Boolean {
        return if (internalTransmissionHolderSet is HolderState.Undefined) false
        else (internalTransmissionHolderSet as HolderState.Initialized).valueSet.contains(key)
    }

    fun updateHolderData(data: Transmission.Data) {
        holderDataReference.update { holderDataReference ->
            holderDataReference[data.identifier()] = data
            holderDataReference
        }
    }

    fun updateHolderDataReferenceToTrack(dataHolderToTrack: String) {

        internalTransmissionHolderSet = when (internalTransmissionHolderSet) {
            is HolderState.Initialized -> {
                val currentSet = (internalTransmissionHolderSet as HolderState.Initialized).valueSet
                require(!currentSet.contains(dataHolderToTrack)) {
                    "Multiple data holders with the same key is not allowed: $dataHolderToTrack"
                }
                HolderState.Initialized(currentSet + dataHolderToTrack)
            }

            HolderState.Undefined -> {
                HolderState.Initialized(setOf(dataHolderToTrack))
            }
        }
    }

    fun registerComputation(key: String, delegate: ComputationOwner) {
        require(!internalComputationMap.containsKey(key)) {
            "Multiple computations with the same key is not allowed: $key"
        }
        internalComputationMap[key] = delegate
    }

    fun hasComputation(type: String): Boolean {
        return internalComputationMap.containsKey(type)
    }

    fun getComputationByKey(type: String): ComputationOwner? {
        return internalComputationMap[type]
    }

    fun getHolderDataByKey(key: String): Transmission.Data? {
        return holderDataReference.value[key]
    }

    fun clear() {
        internalComputationMap.clear()
    }


}

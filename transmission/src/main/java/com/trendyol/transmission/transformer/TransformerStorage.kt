package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.query.ComputationOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TransformerStorage {

    private val holderDataReference: MutableStateFlow<MutableMap<String, Transmission.Data?>> =
        MutableStateFlow(mutableMapOf())

    private var internalTransmissionHolderSet: HolderState = HolderState.Undefined

    private val internalComputationMap: MutableMap<String, ComputationOwner> =
        mutableMapOf()

    fun isHolderStateInitialized(): Boolean {
        return internalTransmissionHolderSet is HolderState.Initialized
    }

    fun isHolderDataDefined(type: String): Boolean {
        return if (internalTransmissionHolderSet is HolderState.Undefined) false
        else (internalTransmissionHolderSet as HolderState.Initialized).valueSet.contains(type)
    }

    fun updateHolderData(data: Transmission.Data) {
        holderDataReference.update { holderDataReference ->
            holderDataReference[data::class.java.simpleName] = data
            holderDataReference
        }
    }

    fun updateHolderDataReferenceToTrack(dataHolderToTrack: String) {

        internalTransmissionHolderSet = when (internalTransmissionHolderSet) {
            is HolderState.Initialized -> {
                val currentSet = (internalTransmissionHolderSet as HolderState.Initialized).valueSet
                require(!currentSet.contains(dataHolderToTrack)) {
                    "Multiple data holders with the same type is not allowed: $dataHolderToTrack"
                }
                HolderState.Initialized(currentSet + dataHolderToTrack)
            }

            HolderState.Undefined -> {
                HolderState.Initialized(setOf(dataHolderToTrack))
            }
        }
    }

    fun registerComputation(name: String, delegate: ComputationOwner) {
        require(!internalComputationMap.containsKey(name)) {
            "Multiple computations with the same type is not allowed: $name"
        }
        internalComputationMap[name] = delegate
    }

    fun hasComputation(type: String): Boolean {
        return internalComputationMap.containsKey(type)
    }

    fun getComputationByType(type: String): ComputationOwner? {
        return internalComputationMap[type]
    }

    fun getHolderDataByType(type: String): Transmission.Data? {
        return holderDataReference.value[type]
    }

    fun clear() {
        internalComputationMap.clear()
    }


}

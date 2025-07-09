package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.dataholder.HolderState
import com.trendyol.transmission.transformer.request.computation.ComputationOwner
import com.trendyol.transmission.transformer.request.execution.ExecutionOwner

internal class TransformerStorage {

    private val holderDataReference: MutableMap<String, Transmission.Data?> = mutableMapOf()

    private var internalTransmissionHolderSet: HolderState = HolderState.Undefined
    private val computationMap: MutableMap<String, ComputationOwner> = mutableMapOf()
    private val executionMap: MutableMap<String, ExecutionOwner> = mutableMapOf()

    fun clearComputations() {
        computationMap.clear()
    }

    fun clearExecutions() {
        executionMap.clear()
    }

    fun isHolderStateInitialized(): Boolean {
        return internalTransmissionHolderSet is HolderState.Initialized
    }

    fun isHolderDataDefined(key: String): Boolean {
        return if (internalTransmissionHolderSet is HolderState.Undefined) false
        else (internalTransmissionHolderSet as HolderState.Initialized).valueSet.contains(key)
    }

    fun updateHolderData(data: Transmission.Data, key: String) {
        holderDataReference[key] = data
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
        require(!computationMap.containsKey(key)) {
            "Multiple computations with the same key is not allowed: $key"
        }
        computationMap[key] = delegate
    }

    fun registerExecution(key: String, delegate: ExecutionOwner) {
        require(!executionMap.containsKey(key)) {
            "Multiple executions with the same key is not allowed: $key"
        }
        executionMap[key] = delegate
    }

    fun hasComputation(type: String): Boolean {
        return computationMap.containsKey(type)
    }

    fun hasExecution(type: String): Boolean {
        return executionMap.containsKey(type)
    }

    fun getComputationByKey(type: String): ComputationOwner.Default? {
        return computationMap[type] as? ComputationOwner.Default
    }

    fun getExecutionByKey(type: String): ExecutionOwner.Default? {
        return executionMap[type] as? ExecutionOwner.Default
    }

    fun <A : Any> getComputationByKey(type: String): ComputationOwner.WithArgs<A>? {
        return computationMap[type] as? ComputationOwner.WithArgs<A>
    }

    fun <A : Any> getExecutionByKey(type: String): ExecutionOwner.WithArgs<A>? {
        return executionMap[type] as? ExecutionOwner.WithArgs<A>
    }

    fun getHolderDataByKey(key: String): Transmission.Data? {
        return holderDataReference[key]
    }

    fun clear() {
        computationMap.clear()
    }
}

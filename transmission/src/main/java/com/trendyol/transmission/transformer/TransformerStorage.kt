package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.dataholder.HolderState
import com.trendyol.transmission.transformer.request.computation.ComputationOwner
import com.trendyol.transmission.transformer.request.execution.ExecutionOwner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class TransformerStorage {

    private val holderDataReference: MutableMap<String, Transmission.Data?> = mutableMapOf()
    private val lock = Mutex()

    private var internalTransmissionHolderSet: HolderState = HolderState.Undefined

    private val internalComputationMap: MutableMap<String, ComputationOwner> =
        mutableMapOf()

    private val internalExecutionMap: MutableMap<String, ExecutionOwner> =
        mutableMapOf()

    fun clearComputations() {
       internalComputationMap.clear()
    }

    fun clearExecutions() {
       internalExecutionMap.clear()
    }

    fun isHolderStateInitialized(): Boolean {
        return internalTransmissionHolderSet is HolderState.Initialized
    }

    fun isHolderDataDefined(key: String): Boolean {
        return if (internalTransmissionHolderSet is HolderState.Undefined) false
        else (internalTransmissionHolderSet as HolderState.Initialized).valueSet.contains(key)
    }

    suspend fun updateHolderData(data: Transmission.Data, key: String) {
        lock.withLock {
            holderDataReference[key] = data
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

    fun registerExecution(key: String, delegate: ExecutionOwner) {
        require(!internalExecutionMap.containsKey(key)) {
            "Multiple executions with the same key is not allowed: $key"
        }
        internalExecutionMap[key] = delegate
    }

    fun hasComputation(type: String): Boolean {
        return internalComputationMap.containsKey(type)
    }

    fun hasExecution(type: String): Boolean {
        return internalExecutionMap.containsKey(type)
    }

    fun getComputationByKey(type: String): ComputationOwner.Default? {
        return internalComputationMap[type] as? ComputationOwner.Default
    }

    fun getExecutionByKey(type: String): ExecutionOwner.Default? {
        return internalExecutionMap[type] as? ExecutionOwner.Default
    }

    fun <A : Any> getComputationByKey(type: String): ComputationOwner.WithArgs<A>? {
        return internalComputationMap[type] as? ComputationOwner.WithArgs<A>
    }

    fun <A : Any> getExecutionByKey(type: String): ExecutionOwner.WithArgs<A>? {
        return internalExecutionMap[type] as? ExecutionOwner.WithArgs<A>
    }

    fun getHolderDataByKey(key: String): Transmission.Data? {
        return holderDataReference[key]
    }

    fun clear() {
        internalComputationMap.clear()
    }
}

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

/**
 * Interface for managing mutable state within transformers in a thread-safe manner.
 * 
 * TransmissionDataHolder provides state management capabilities for transformers, allowing
 * safe concurrent access and modification of transformer-specific data. The holder automatically
 * handles synchronization and can optionally publish updates to the router's data stream.
 * 
 * @param T The type of data being held, must extend [Transmission.Data] or be nullable
 * 
 * @see dataHolder for creating instances
 */
interface TransmissionDataHolder<T : Transmission.Data?> {
    /**
     * Gets the current value held by this data holder.
     * 
     * This operation is thread-safe and returns the most recent value.
     * 
     * @return The current value of type [T]
     * 
     * Example usage:
     * ```kotlin
     * val currentUser = userState.getValue()
     * if (currentUser.isLoggedIn) {
     *     // Handle logged in state
     * }
     * ```
     */
    fun getValue(): T
    
    /**
     * Updates the held value using the provided updater function.
     * 
     * This method applies the updater function to the current value and stores the result.
     * The operation is thread-safe and atomic. If `publishUpdates` was enabled during creation,
     * the updated value will be automatically sent to the router's data stream.
     * 
     * @param updater Function that takes the current value and returns the new value
     * 
     * Example usage:
     * ```kotlin
     * userState.update { currentState ->
     *     currentState.copy(isLoggedIn = true, user = newUser)
     * }
     * ```
     */
    fun update(updater: (T) -> @UnsafeVariance T)
    
    /**
     * Updates the held value and returns the new value atomically.
     * 
     * This method combines [update] and [getValue] operations in a single atomic operation.
     * It applies the updater function and returns the resulting value, ensuring consistency
     * in concurrent scenarios.
     * 
     * @param updater Function that takes the current value and returns the new value
     * @return The new value after applying the updater function
     * 
     * Example usage:
     * ```kotlin
     * val newUserState = userState.updateAndGet { currentState ->
     *     currentState.copy(loginAttempts = currentState.loginAttempts + 1)
     * }
     * 
     * if (newUserState.loginAttempts > maxAttempts) {
     *     // Handle too many login attempts
     * }
     * ```
     */
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

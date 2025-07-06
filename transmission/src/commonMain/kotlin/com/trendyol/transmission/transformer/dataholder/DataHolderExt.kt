package com.trendyol.transmission.transformer.dataholder

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler

/**
 * Creates a data holder for managing state within a transformer.
 * 
 * Data holders provide thread-safe state management within transformers, allowing you to:
 * - Store and update transformer-specific data
 * - Automatically publish updates to the router's data stream
 * - Enable other transformers to query the held data via contracts
 * - Maintain state across multiple signal/effect processing cycles
 * 
 * The data holder uses a mutex internally to ensure thread-safe updates and provides
 * both synchronous and asynchronous update methods.
 * 
 * @param T The type of data to hold, must extend [Transmission.Data] or be nullable
 * @param initialValue Initial value for the data holder
 * @param contract Optional contract for enabling inter-transformer queries. If null, a random contract is generated.
 * @param publishUpdates Whether to automatically send updates to the router's data stream
 * @return A [TransmissionDataHolder] instance for managing the data
 * 
 * @throws IllegalArgumentException when multiple data holders with the same type are defined inside a [Transformer]
 * 
 * Example usage:
 * ```kotlin
 * class UserTransformer : Transformer() {
 *     private val userState = dataHolder(
 *         initialValue = UserState(isLoggedIn = false),
 *         publishUpdates = true
 *     )
 *     
 *     override val handlers = handlers {
 *         onSignal<UserSignal.Login> { signal ->
 *             val user = authenticateUser(signal.credentials)
 *             userState.update { it.copy(isLoggedIn = true, user = user) }
 *             // Update is automatically published to data stream
 *         }
 *     }
 * }
 * ```
 * 
 * @see TransmissionDataHolder for update operations
 * @see Contract.DataHolder for enabling inter-transformer queries
 */
fun <T : Transmission.Data?> Transformer.dataHolder(
    initialValue: T,
    contract: Contract.DataHolder<T>? = null,
    publishUpdates: Boolean = true
): TransmissionDataHolder<T> {
    return TransmissionDataHolderImpl(
        initialValue = initialValue,
        publishUpdates = publishUpdates,
        transformer = this,
        contract = contract ?: Contract.dataHolder(),
    )
}

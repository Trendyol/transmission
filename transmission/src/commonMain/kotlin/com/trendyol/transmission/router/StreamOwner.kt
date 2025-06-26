package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.flow.SharedFlow

/**
 * Interface for components that own and provide access to data and effect streams.
 * 
 * StreamOwner defines the contract for objects that can emit [Transmission.Data] and 
 * [Transmission.Effect] through reactive streams. This interface is primarily implemented 
 * by [TransmissionRouter] to provide stream access to consumers.
 * 
 * @see TransmissionRouter
 * @see streamData extension functions for convenient stream consumption
 */
interface StreamOwner {
    /**
     * Stream of all [Transmission.Data] emitted by transformers within this router.
     * Data represents the final processed state after signal/effect handling.
     */
    val dataStream: SharedFlow<Transmission.Data>
    
    /**
     * Stream of all [Transmission.Effect] emitted by transformers within this router.
     * Effects represent side effects that can trigger additional processing in other transformers.
     */
    val effectStream: SharedFlow<Transmission.Effect>
}
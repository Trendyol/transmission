package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.checkpoint.CheckpointHandler
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.QueryHandler

interface CommunicationScope : QueryHandler, CheckpointHandler {
    /**
     * Sends data to [TransmissionRouter]
     * @param data of type [Transmission.Data]
     */
    suspend fun <D : Transmission.Data> send(data: D?)

    /**
     * Publishes [Transmission.Effect] to other [Transformer]s
     * @param effect of type [Transmission.Effect]
     */
    suspend fun <E : Transmission.Effect> publish(effect: E)

    /**
     * Sends arbitrary payload to [TransmissionRouter] via [Transmission.Effect] internally.
     * These can be observed via one-shot payload observers
     * @param payload Arbitrary data to send
     */
    @ExperimentalTransmissionApi
    suspend fun <D: Any> sendPayload(payload: D)

    /**
     * Sends [Transmission.Effect] to a specific [Transformer]
     * @param effect of type [Transmission.Effect]
     * @param to Target [Transformer] identity Contract
     */
    suspend fun <E : Transmission.Effect> send(effect: E, identity: Contract.Identity)
}

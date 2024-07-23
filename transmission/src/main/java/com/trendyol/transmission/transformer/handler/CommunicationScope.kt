package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.RequestHandler

interface CommunicationScope : RequestHandler {
    /**
     * Sends data to [TransmissionRouter]
     * @param data of type [Transmission.Data]
     */
    fun <D : Transmission.Data> send(data: D?)

    /**
     * Publishes [Transmission.Effect] to other [Transformer]s
     * @param effect of type [Transmission.Effect]
     */
    fun <E : Transmission.Effect> publish(effect: E)

    /**
     * Sends [Transmission.Effect] to a specific [Transformer]
     * @param effect of type [Transmission.Effect]
     * @param to Target [Transformer] identity Contract
     */
    fun <E : Transmission.Effect> send(effect: E, identity: Contract.Identity)
}

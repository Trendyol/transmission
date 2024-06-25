package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.RequestHandler
import kotlin.reflect.KClass

interface CommunicationScope: RequestHandler {
	/**
	 * Sends data to [TransmissionRouter]
	 * @param data of type [Transmission.Data]
	 */
	fun<D: Transmission.Data> send(data: D?)

	/**
	 * Publishes [Transmission.Effect] to other [Transformer]s
	 * @param effect of type [Transmission.Effect]
	 */
	fun<E: Transmission.Effect> publish(effect: E)

	/**
	 * Sends [Transmission.Effect] to a specific [Transformer]
	 * @param effect of type [Transmission.Effect]
	 * @param to Target [Transformer]
	 */
	fun <E: Transmission.Effect, T: Transformer>send(effect: E, to: KClass<out T>)
}

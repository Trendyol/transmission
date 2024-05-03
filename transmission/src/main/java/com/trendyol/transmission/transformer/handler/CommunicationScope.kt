package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.TransmissionRouter
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender
import kotlin.reflect.KClass

typealias usingCommunication = CommunicationScope<Transmission.Data, Transmission.Effect>

interface CommunicationScope<D : Transmission.Data, E : Transmission.Effect>: QuerySender<D,E> {
	/**
	 * Sends data to [TransmissionRouter]
	 * @param data of type [Transmission.Data]
	 */
	fun send(data: D?)

	/**
	 * Publishes [Transmission.Effect] to other [Transformer]s
	 * @param effect of type [Transmission.Effect]
	 */
	fun publish(effect: E)

	/**
	 * Sends [Transmission.Effect] to a specific [Transformer]
	 * @param effect of type [Transmission.Effect]
	 * @param to Target [Transformer]
	 */
	fun send(effect: E, to: KClass<out Transformer<D, E>>)
}

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.query.QuerySender
import kotlin.reflect.KClass

interface CommunicationScope<D : Transmission.Data, E : Transmission.Effect>: QuerySender<D,E> {
	fun publishData(data: D?)
	fun publishEffect(effect: E)
	fun sendEffect(effect: E, to: KClass<out Transformer<D, E>>)
}

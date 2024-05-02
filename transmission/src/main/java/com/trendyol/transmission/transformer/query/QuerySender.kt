package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

interface QuerySender<D : Transmission.Data, E : Transmission.Effect>{
	suspend fun <D : Transmission.Data> queryData(type: KClass<D>): D?
	suspend fun <D : Transmission.Data, TD: Transmission.Data, T : Transformer<TD, E>> queryData(
		type: KClass<D>,
		owner: KClass<out T>
	): D?
}

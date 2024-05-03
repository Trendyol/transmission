package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

sealed class QueryResponse<D : Transmission.Data>(open val owner: String, open val data: D?) {
	data class Data<D : Transmission.Data>(override val owner: String, override val data: D?) :
		QueryResponse<D>(owner, data)

	data class Computation<D : Transmission.Data>(
		override val owner: String,
		override val data: D?
	) : QueryResponse<D>(owner, data)
}

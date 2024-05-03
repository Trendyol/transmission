package com.trendyol.transmission.transformer.query

sealed interface Query {
	data class Data(val sender: String, val dataOwner: String? = null, val type: String) : Query
	data class Computation(
		val sender: String,
		val computationOwner: String,
		val type: String,
		val invalidate: Boolean = false,
	) : Query
}

package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

data class QueryResponse<D: Transmission.Data>(val owner: String, val data: D?)

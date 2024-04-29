package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

interface HandlerScope<D: Transmission.Data, E: Transmission.Effect> {
	fun publishData(data: D?)
	fun publishEffect(effect: E)
}

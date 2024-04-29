package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

interface HandlerScope<D: Transmission.Data> {
	fun publishData(data: D?)
	fun publishEffect(effect: Transmission.Effect)
}

package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

interface HandlerScope {
	fun publishData(data: Transmission.Data?)
	fun publishEffect(effect: Transmission.Effect)
}

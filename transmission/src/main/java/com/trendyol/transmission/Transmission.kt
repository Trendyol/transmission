package com.trendyol.transmission

sealed interface Transmission {
	interface Signal : Transmission
	interface Effect : Transmission
	interface Data : Transmission
}

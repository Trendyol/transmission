package com.trendyol.transmission.transformer

sealed interface HolderState {
	data object Undefined: HolderState
	data class Initialized(val value: String): HolderState
}

package com.trendyol.transmission.transformer

sealed interface HolderState {
	data object Undefined: HolderState
	data class Initialized(val valueSet: Set<String>): HolderState
}

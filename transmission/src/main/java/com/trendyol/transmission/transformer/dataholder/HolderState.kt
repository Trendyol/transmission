package com.trendyol.transmission.transformer.dataholder

internal sealed interface HolderState {
	data object Undefined: HolderState
	data class Initialized(val valueSet: Set<String>): HolderState
}

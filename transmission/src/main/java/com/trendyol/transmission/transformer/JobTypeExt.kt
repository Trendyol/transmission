package com.trendyol.transmission.transformer

import kotlinx.coroutines.Job

internal fun MutableList<Job?>.clearJobs() {
	this.forEach { it?.cancel() }
	this.clear()
}

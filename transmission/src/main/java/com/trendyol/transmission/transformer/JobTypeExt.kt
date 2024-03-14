package com.trendyol.transmission.transformer

import kotlinx.coroutines.Job

internal fun MutableMap<JobType, Job?>.update(key: JobType, newJob : () -> Job) {
	this[key]?.cancel()
	this[key] = newJob()
}

internal fun MutableMap<JobType, Job?>.clearJobs() {
	this.values.forEach { it?.cancel() }
	this.clear()
}

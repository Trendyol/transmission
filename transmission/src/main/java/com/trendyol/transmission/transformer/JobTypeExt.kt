package com.trendyol.transmission.transformer

import kotlinx.coroutines.Job

internal fun MutableMap<JobType, Job?>.update(key: String, newJob : () -> Job) {
	val jobType =JobType(key)
	this[jobType]?.cancel()
	this[jobType] = newJob()
}

internal fun MutableMap<JobType, Job?>.clearJobs() {
	this.values.forEach { it?.cancel() }
	this.clear()
}

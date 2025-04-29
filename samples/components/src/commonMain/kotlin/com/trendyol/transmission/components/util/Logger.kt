package com.trendyol.transmission.components.util

object Logger {
    fun d(tag: String, message: String) = logDebug(tag, message)
}

expect fun logDebug(tag: String, message: String)
package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.flow.Flow

interface StreamOwner {
    val dataStream: Flow<Transmission.Data>
    val effectStream: Flow<Transmission.Effect>
}
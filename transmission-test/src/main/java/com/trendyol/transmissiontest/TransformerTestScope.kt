package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import kotlinx.coroutines.flow.Flow

interface TransformerTestScope {
    val dataStream: Flow<Transmission.Data>
    val effectStream: Flow<Transmission.Effect>
}

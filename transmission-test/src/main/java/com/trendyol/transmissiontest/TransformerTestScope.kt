package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission

interface TransformerTestScope {
    val dataStream: List<Transmission.Data>
    val effectStream: List<Transmission.Effect>
}

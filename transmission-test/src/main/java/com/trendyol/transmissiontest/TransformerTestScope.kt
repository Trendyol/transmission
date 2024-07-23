package com.trendyol.transmissiontest

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper

interface TransformerTestScope {
    val dataStream: List<Transmission.Data>
    val effectStream: List<EffectWrapper>
}

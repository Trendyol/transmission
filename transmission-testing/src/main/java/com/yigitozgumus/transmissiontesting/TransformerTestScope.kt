package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper

interface TransformerTestScope {
    val dataStream: List<Transmission.Data>
    val effectStream: List<EffectWrapper>
    fun sendSignal(signal: Transmission.Signal)
    fun sendEffect(effect: Transmission.Effect)
}

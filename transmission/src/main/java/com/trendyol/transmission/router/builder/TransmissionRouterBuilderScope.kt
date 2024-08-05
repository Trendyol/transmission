package com.trendyol.transmission.router.builder

import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher

interface TransmissionRouterBuilderScope {
    fun withDispatcher(dispatcher: CoroutineDispatcher)
    fun withTransformerSet(transformerSet: Set<Transformer>)
}

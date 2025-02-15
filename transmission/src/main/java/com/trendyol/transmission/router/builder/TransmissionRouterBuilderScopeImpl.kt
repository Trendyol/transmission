package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.Capacity
import com.trendyol.transmission.router.loader.TransformerSetLoader
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class TransmissionRouterBuilderScopeImpl internal constructor(
    scope: TransmissionRouterBuilderScope.() -> Unit
) {

    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default
    internal lateinit var transformerSetLoader: TransformerSetLoader
    internal var autoInitialization: Boolean = true
    internal var capacity: Capacity = Capacity.Default

    private val scopeImpl = object : TransmissionRouterBuilderScope {

        override fun addDispatcher(dispatcher: CoroutineDispatcher) {
            this@TransmissionRouterBuilderScopeImpl.dispatcher = dispatcher
        }

        override fun addTransformerSet(transformerSet: Set<Transformer>) {
            val loader = object : TransformerSetLoader {
                override suspend fun load(): Set<Transformer> {
                    return transformerSet
                }
            }
            addLoader(loader)
        }

        override fun setCapacity(capacity: Capacity) {
            this@TransmissionRouterBuilderScopeImpl.capacity = capacity
        }

        override fun addLoader(loader: TransformerSetLoader) {
            this@TransmissionRouterBuilderScopeImpl.transformerSetLoader = loader
        }

        override fun overrideInitialization() {
            this@TransmissionRouterBuilderScopeImpl.autoInitialization = false
        }
    }

    init {
        scopeImpl.apply(scope)
    }
}

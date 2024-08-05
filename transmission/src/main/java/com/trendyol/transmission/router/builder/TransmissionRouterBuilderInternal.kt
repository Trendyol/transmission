package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.RegistryScope
import com.trendyol.transmission.router.RegistryScopeImpl
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class TransmissionRouterBuilderInternal internal constructor(
    scope: TransmissionTestingRouterBuilderScope.() -> Unit
) {

    internal var dispatcher: CoroutineDispatcher = Dispatchers.Default
    internal var transformerSet = setOf<Transformer>()
    internal var registryScope: RegistryScopeImpl? = null

    private val scopeImpl = object : TransmissionTestingRouterBuilderScope {

        override fun testing(scope: RegistryScope.() -> Unit) {
            this@TransmissionRouterBuilderInternal.registryScope = RegistryScopeImpl().apply(scope)
        }

        override fun withDispatcher(dispatcher: CoroutineDispatcher) {
            this@TransmissionRouterBuilderInternal.dispatcher = dispatcher

        }

        override fun withTransformerSet(transformerSet: Set<Transformer>) {
            this@TransmissionRouterBuilderInternal.transformerSet = transformerSet
        }
    }

    init {
        scopeImpl.apply(scope)
    }
}

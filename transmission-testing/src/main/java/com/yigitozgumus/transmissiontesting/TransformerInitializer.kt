package com.yigitozgumus.transmissiontesting

import app.cash.turbine.turbineScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.ExperimentalCoroutinesApi


suspend fun Transformer.testWith(
    signal: Transmission.Signal,
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.() -> Unit = {}
) {
    val registryImpl = RegistryScopeImpl().apply(registry)
    val scopeImpl =
        TransformerTestScopeImpl(registryImpl, this).apply {
            acceptSignal(signal)
            turbineScope {
                scope(this@apply)
            }
        }
    scopeImpl.clear()
}

suspend fun  Transformer.testWith(
    effect: Transmission.Effect,
    registry: RegistryScope.() -> Unit = {},
    scope: suspend TransformerTestScope.() -> Unit = {}
) {
    val registryImpl = RegistryScopeImpl().apply(registry)
    val scopeImpl =
        TransformerTestScopeImpl(registryImpl, this).apply {
            acceptEffect(effect)
            turbineScope {
                scope(this@apply)
            }
        }
    scopeImpl.clear()
}

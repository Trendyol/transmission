package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.ExperimentalCoroutinesApi


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <D : Transmission.Data, E : Transmission.Effect> Transformer<D, E>.testWith(
    signal: Transmission.Signal,
    registry: RegistryScope<D, E, Transformer<D, E>>.() -> Unit = {},
    scope: suspend TransformerTestScope<D, E>.() -> Unit = {}
) {
    val registryImpl = RegistryScopeImpl<D, E, Transformer<D, E>>().apply(registry)
    val scopeImpl =
        TransformerTestScopeImpl(registryImpl, this).apply {
            acceptSignal(signal)
            scope(this)
        }
    scopeImpl.clear()
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <D : Transmission.Data, E : Transmission.Effect> Transformer<D, E>.testWith(
    effect: E,
    registry: RegistryScope<D, E, Transformer<D, E>>.() -> Unit = {},
    scope: suspend TransformerTestScope<D, E>.() -> Unit = {}
) {
    val registryImpl = RegistryScopeImpl<D, E, Transformer<D, E>>().apply(registry)
    val scopeImpl =
        TransformerTestScopeImpl(registryImpl, this).apply {
            acceptEffect(effect)
            scope(this)
        }
    scopeImpl.clear()
}

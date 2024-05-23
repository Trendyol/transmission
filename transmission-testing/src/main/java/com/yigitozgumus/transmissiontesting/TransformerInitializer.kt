package com.yigitozgumus.transmissiontesting

import app.cash.turbine.TurbineContext
import app.cash.turbine.turbineScope
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher


@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <D : Transmission.Data, E : Transmission.Effect> Transformer<D, E>.testWith(
    dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
    registry: RegistryScope<D, E, Transformer<D, E>>.() -> Unit = {},
    scope: suspend TransformerTestScope<D, E>.() -> Unit = {}
) {
    val registryImpl = RegistryScopeImpl<D, E, Transformer<D, E>>().apply(registry)
    val scopeImpl =
        TransformerTestScopeImpl(dispatcher, registryImpl, this@testWith).apply {
            scope(this)
        }
    scopeImpl.clear()
}
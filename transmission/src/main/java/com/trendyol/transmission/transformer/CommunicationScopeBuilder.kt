package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.query.TransformerQueryDelegate
import kotlinx.coroutines.channels.Channel
import kotlin.reflect.KClass

internal class CommunicationScopeBuilder(
    private val effectChannel: Channel<EffectWrapper>,
    private val dataChannel: Channel<Transmission.Data>,
    private val queryDelegate: TransformerQueryDelegate
) : CommunicationScope {

    override fun <D : Transmission.Data> send(data: D?) {
        data?.let { dataChannel.trySend(it) }
    }

    override fun <E : Transmission.Effect, T : Transformer> send(effect: E, to: KClass<out T>) {
        effectChannel.trySend(EffectWrapper(effect, to))
    }

    override fun <E : Transmission.Effect> publish(effect: E) {
        effectChannel.trySend(EffectWrapper(effect))
    }

    override suspend fun <D : Transmission.Data> queryData(key: String): D? {
        return queryDelegate.interactor.queryData(key)
    }

    override suspend fun <D : Transmission.Data> queryComputation(
        key: String,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.queryComputation(key, invalidate)
    }

    override suspend fun <A : Any, D : Transmission.Data> queryComputationWithArgs(
        args: A,
        key: String,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.queryComputationWithArgs(args, key, invalidate)
    }
}

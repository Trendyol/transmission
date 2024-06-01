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

    override suspend fun <D : Transmission.Data> queryData(
        type: KClass<D>,
        owner: KClass<out Transformer>?
    ): D? {
        return queryDelegate.interactor.queryData(type, owner)
    }

    override suspend fun <D : Transmission.Data, T : Transformer> queryComputation(
        type: KClass<D>,
        owner: KClass<out T>,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.queryComputation(type, owner, invalidate)
    }
}
package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.query.Contract
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

    override suspend fun <C : Contract.Data<D>, D : Transmission.Data> queryData(contract: C): D? {
        return queryDelegate.interactor.queryData(contract)
    }

    override suspend fun <C : Contract.Computation<D>, D : Transmission.Data> queryComputation(
        contract: C,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.queryComputation(contract, invalidate)
    }

    override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Transmission.Data> queryComputationWithArgs(
        contract: C,
        args: A,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.queryComputationWithArgs(contract, args, invalidate)
    }

}

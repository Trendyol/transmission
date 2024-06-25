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

    override suspend fun <C : Contract.Data<D>, D : Transmission.Data> getData(contract: C): D? {
        return queryDelegate.interactor.getData(contract)
    }

    override suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.compute(contract, invalidate)
    }

    override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean
    ): D? {
        return queryDelegate.interactor.compute(contract, args, invalidate)
    }

    override suspend fun <C : Contract.Execution> execute(contract: C, invalidate: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
        contract: C,
        args: A,
        invalidate: Boolean
    ) {
        TODO("Not yet implemented")
    }
}

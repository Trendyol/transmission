package com.trendyol.transmission.transformer

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.TransformerRequestDelegate
import kotlinx.coroutines.channels.Channel

internal class CommunicationScopeBuilder(
    private val effectChannel: Channel<EffectWrapper>,
    private val dataChannel: Channel<Transmission.Data>,
    private val requestDelegate: TransformerRequestDelegate
) : CommunicationScope {

    override suspend fun <D : Transmission.Data> send(data: D?) {
        data?.let { dataChannel.send(it) }
    }

    override suspend fun <E : Transmission.Effect> send(
        effect: E,
        identity: Contract.Identity
    ) {
        effectChannel.send(EffectWrapper(effect, identity))
    }

    override suspend fun <E : Transmission.Effect> publish(effect: E) {
        effectChannel.send(EffectWrapper(effect))
    }

    override suspend fun <C : Contract.DataHolder<D>, D : Transmission.Data> getData(contract: C): D? {
        return requestDelegate.interactor.getData(contract)
    }

    override suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean
    ): D? {
        return requestDelegate.interactor.compute(contract, invalidate)
    }

    override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean
    ): D? {
        return requestDelegate.interactor.compute(contract, args, invalidate)
    }

    override suspend fun execute(contract: Contract.Execution) {
        requestDelegate.interactor.execute(contract)
    }

    override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
        contract: C,
        args: A
    ) {
        requestDelegate.interactor.execute(contract, args)
    }
}

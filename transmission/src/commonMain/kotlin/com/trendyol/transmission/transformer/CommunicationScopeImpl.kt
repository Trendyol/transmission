package com.trendyol.transmission.transformer

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.RouterEffectWithType
import com.trendyol.transmission.effect.WrappedEffect
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.TransformerQueryDelegate
import kotlinx.coroutines.channels.Channel

@OptIn(ExperimentalTransmissionApi::class)
internal class CommunicationScopeImpl(
    private val effectChannel: Channel<WrappedEffect>,
    private val dataChannel: Channel<Transmission.Data>,
    private val queryDelegate: TransformerQueryDelegate
) : CommunicationScope {

    override suspend fun <D : Transmission.Data> send(data: D?) {
        data?.let { dataChannel.send(it) }
    }

    override suspend fun <D : Any> sendPayload(payload: D) {
        effectChannel.send(WrappedEffect(RouterEffectWithType<D>(payload)))
    }

    override suspend fun <E : Transmission.Effect> send(
        effect: E,
        identity: Contract.Identity
    ) {
        effectChannel.send(WrappedEffect(effect, identity))
    }

    override suspend fun <E : Transmission.Effect> publish(effect: E) {
        effectChannel.send(WrappedEffect(effect))
    }

    override suspend fun <D : Transmission.Data?> getData(contract: Contract.DataHolder<D>): D {
        return queryDelegate.queryHandler.getData(contract)
    }

    override suspend fun <D : Any?> compute(
        contract: Contract.Computation<D>,
        invalidate: Boolean
    ): D {
        return queryDelegate.queryHandler.compute(contract, invalidate)
    }

    override suspend fun <A : Any, D : Any?> compute(
        contract: Contract.ComputationWithArgs<A, D>,
        args: A,
        invalidate: Boolean
    ): D {
        return queryDelegate.queryHandler.compute(contract, args, invalidate)
    }

    override suspend fun execute(contract: Contract.Execution) {
        queryDelegate.queryHandler.execute(contract)
    }

    override suspend fun <A : Any> execute(
        contract: Contract.ExecutionWithArgs<A>,
        args: A
    ) {
        queryDelegate.queryHandler.execute(contract, args)
    }

    @ExperimentalTransmissionApi
    override suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint.Default
    ) {
        with(queryDelegate.checkpointHandler) {
            pauseOn(contract)
        }
    }

    @ExperimentalTransmissionApi
    override suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint.Default
    ) {
        with(queryDelegate.checkpointHandler) {
            pauseOn(*contract)
        }
    }

    @ExperimentalTransmissionApi
    override suspend fun <A : Any> CommunicationScope.pauseOn(
        contract: Contract.Checkpoint.WithArgs<A>
    ): A {
        return with(queryDelegate.checkpointHandler) {
            pauseOn(contract)
        }
    }

    override suspend fun validate(contract: Contract.Checkpoint.Default) {
        queryDelegate.checkpointHandler.validate(contract)
    }

    override suspend fun <A : Any> validate(
        contract: Contract.Checkpoint.WithArgs<A>,
        args: A
    ) {
        queryDelegate.checkpointHandler.validate(contract, args)
    }
}

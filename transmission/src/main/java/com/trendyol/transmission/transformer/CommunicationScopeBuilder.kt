package com.trendyol.transmission.transformer

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.Transmission
import com.trendyol.transmission.effect.EffectWrapper
import com.trendyol.transmission.transformer.handler.CommunicationScope
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.TransformerRequestDelegate
import kotlinx.coroutines.channels.Channel

@OptIn(ExperimentalTransmissionApi::class)
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
        return requestDelegate.requestHandler.getData(contract)
    }

    override suspend fun <C : Contract.Computation<D>, D : Any> compute(
        contract: C,
        invalidate: Boolean
    ): D? {
        return requestDelegate.requestHandler.compute(contract, invalidate)
    }

    override suspend fun <C : Contract.ComputationWithArgs<A, D>, A : Any, D : Any> compute(
        contract: C,
        args: A,
        invalidate: Boolean
    ): D? {
        return requestDelegate.requestHandler.compute(contract, args, invalidate)
    }

    override suspend fun execute(contract: Contract.Execution) {
        requestDelegate.requestHandler.execute(contract)
    }

    override suspend fun <C : Contract.ExecutionWithArgs<A>, A : Any> execute(
        contract: C,
        args: A
    ) {
        requestDelegate.requestHandler.execute(contract, args)
    }

    override suspend fun CommunicationScope.pauseOn(
        contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    ) {
        with(requestDelegate.checkpointHandler) {
            pauseOn(contract, resumeBlock)
        }
    }

    override suspend fun CommunicationScope.pauseOn(
        vararg contract: Contract.Checkpoint,
        resumeBlock: suspend CommunicationScope.() -> Unit
    ) {
        with(requestDelegate.checkpointHandler) {
            pauseOn(contract = contract, resumeBlock = resumeBlock)
        }
    }

    override suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> CommunicationScope.pauseOn(
        contract: C,
        resumeBlock: suspend CommunicationScope.(args: A) -> Unit
    ) {
        with(requestDelegate.checkpointHandler) {
            pauseOn(contract, resumeBlock)
        }
    }

    @ExperimentalTransmissionApi
    override suspend fun <C : Contract.CheckpointWithArgs<A>, C2 : Contract.CheckpointWithArgs<B>, A : Any, B : Any> CommunicationScope.pauseOn(
        contract: C,
        contract2: C2,
        resumeBlock: suspend CommunicationScope.(A, B) -> Unit
    ) {
        with(requestDelegate.checkpointHandler) {
            pauseOn(contract, contract2, resumeBlock)
        }
    }

    override suspend fun validate(contract: Contract.Checkpoint) {
        requestDelegate.checkpointHandler.validate(contract)
    }

    override suspend fun <C : Contract.CheckpointWithArgs<A>, A : Any> validate(
        contract: C,
        args: A
    ) {
        requestDelegate.checkpointHandler.validate(contract, args)
    }
}

package com.trendyol.transmissiontest.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckpointWithArgsTransformer<A: Any>(
    checkpoint: Contract.Checkpoint.WithArgs<A>, args: () -> A
): Transformer(dispatcher = UnconfinedTestDispatcher()) {
    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = handlers {
        onSignal<CheckpointWithArgs<A>> {
            validate(checkpoint, args())
        }
    }
}

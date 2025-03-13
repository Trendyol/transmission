package com.trendyol.transmissiontest.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.createHandlers
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckpointWithArgsTransformer<C: Contract.Checkpoint.WithArgs<A>, A: Any>(
    checkpoint: C, args: () -> A
): Transformer(dispatcher = UnconfinedTestDispatcher()) {
    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = createHandlers {
        onSignal<CheckpointWithArgs<C, A>> {
            validate(checkpoint, args())
        }
    }
}

package com.trendyol.transmissiontest.checkpoint

import com.trendyol.transmission.ExperimentalTransmissionApi
import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.createHandlers
import com.trendyol.transmission.transformer.handler.onEffect
import com.trendyol.transmission.transformer.handler.onSignal
import com.trendyol.transmission.transformer.request.Contract
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
internal class CheckpointTransformer(
    checkpointProvider: () -> Contract.Checkpoint.Default
): Transformer(dispatcher = UnconfinedTestDispatcher()) {
    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = createHandlers {
        onSignal<DefaultCheckPoint> {
            validate(checkpointProvider())
        }
    }
}

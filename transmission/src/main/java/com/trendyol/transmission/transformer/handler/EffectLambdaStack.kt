package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

typealias EffectLambda = suspend CommunicationScope.(effect: Transmission.Effect) -> Unit

class EffectLambdaStack {
    private var stackedLambda: EffectLambda = { _ -> }

    fun addOperation(operation: EffectLambda): EffectLambdaStack {
        val previousLambda = stackedLambda
        stackedLambda = { effect ->
            previousLambda(this, effect)
            operation(this, effect)
        }
        return this
    }

    suspend fun execute(scope: CommunicationScope, effect: Transmission.Effect) {
        stackedLambda(scope, effect)
    }
}

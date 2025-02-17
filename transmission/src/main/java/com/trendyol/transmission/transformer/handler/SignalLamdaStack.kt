package com.trendyol.transmission.transformer.handler

import com.trendyol.transmission.Transmission

typealias SignalLambda = suspend CommunicationScope.(signal: Transmission.Signal) -> Unit

class SignalLambdaStack {
    private var stackedLambda: SignalLambda = { _ -> }

    fun addOperation(operation: SignalLambda): SignalLambdaStack {
        val previousLambda = stackedLambda
        stackedLambda = { signal ->
            previousLambda(this, signal)
            operation(this, signal)
        }
        return this
    }

    suspend fun execute(scope: CommunicationScope, signal: Transmission.Signal) {
        stackedLambda(scope, signal)
    }
}

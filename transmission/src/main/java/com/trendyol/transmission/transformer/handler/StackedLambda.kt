package com.trendyol.transmission.transformer.handler

typealias TransmissionLambda<T> = suspend CommunicationScope.(signal: T) -> Unit

class StackedLambda<T>{
    private var stackedLambda: TransmissionLambda<T> = { _ -> }

    fun addOperation(operation: TransmissionLambda<T>) {
        val previousLambda = stackedLambda
        stackedLambda = { signal ->
            previousLambda(this, signal)
            operation(this, signal)
        }
    }

    suspend fun execute(scope: CommunicationScope, transmission: T) {
        stackedLambda(scope, transmission)
    }
}

package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.RequestHandler

class ExecutionBuilder {

    fun buildWith(
        key: String,
        transformer: Transformer,
        execution: suspend RequestHandler.() -> Unit
    ) {
        transformer.storage.registerExecution(
            key = key,
            delegate = ExecutionDelegate(execution)
        )
    }

    fun <A : Any> buildWith(
        key: String,
        transformer: Transformer,
        execution: suspend RequestHandler.(args: A) -> Unit
    ) {
        transformer.storage.registerExecution(
            key = key,
            delegate = ExecutionDelegateWithArgs(execution)
        )
    }
}

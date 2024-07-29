package com.trendyol.transmission.transformer.request.execution

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.request.RequestHandler

class ExecutionRegistry internal constructor(private val transformer: Transformer) {

    internal fun buildWith(
        key: String,
        execution: suspend RequestHandler.() -> Unit
    ) {
        transformer.storage.registerExecution(
            key = key,
            delegate = ExecutionDelegate(execution)
        )
    }

    internal fun <A : Any> buildWith(
        key: String,
        execution: suspend RequestHandler.(args: A) -> Unit
    ) {
        transformer.storage.registerExecution(
            key = key,
            delegate = ExecutionDelegateWithArgs(execution)
        )
    }
}

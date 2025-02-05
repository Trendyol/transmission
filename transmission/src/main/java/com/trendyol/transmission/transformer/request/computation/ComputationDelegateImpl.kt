package com.trendyol.transmission.transformer.request.computation

import com.trendyol.transmission.transformer.request.RequestHandler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ComputationDelegate(
    private val useCache: Boolean = false,
    private val computation: (suspend RequestHandler.() -> Any?)? = null,
) : ComputationOwner.Default {

    private var result: Any? = null

    private val lock = Mutex()

    override suspend fun getResult(scope: RequestHandler, invalidate: Boolean): Any? {
        return if (useCache && invalidate.not()) {
            result ?: lock.withLock { computation?.invoke(scope) }.also { result = it }
        } else {
            result = null
            lock.withLock { computation?.invoke(scope) }
        }
    }
}

internal class ComputationDelegateWithArgs<A : Any>(
    private val useCache: Boolean = false,
    private val computation: suspend RequestHandler.(args: A) -> Any?,
) : ComputationOwner.WithArgs<A> {

    private var result: Any? = null
    private val lock = Mutex()

    override suspend fun getResult(scope: RequestHandler, invalidate: Boolean, args: A): Any? {
        return if (useCache && invalidate.not()) {
            result ?: lock.withLock { computation(scope, args) }.also { result = it }
        } else {
            result = null
            lock.withLock { computation(scope, args) }
        }
    }
}

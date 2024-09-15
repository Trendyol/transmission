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
        return lock.withLock {
            if (useCache && invalidate.not()) {
                result ?: computation?.invoke(scope).also { result = it }
            } else {
                result = null
                computation?.invoke(scope)
            }
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
        return lock.withLock {
            if (useCache && invalidate.not()) {
                result ?: computation(scope, args).also { lock.withLock { result = it } }
            } else {
                result = null
                computation(scope, args)
            }
        }
    }
}

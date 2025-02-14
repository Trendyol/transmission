package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.request.Contract

fun TransmissionRouter(
    identity: Contract.Identity = Contract.identity(),
    scope: TransmissionRouterBuilderScope.() -> Unit
): TransmissionRouter {
    val builder = TransmissionRouterBuilderScopeImpl(scope)
    return TransmissionRouter(
        identity = identity,
        transformerSetLoader =
            builder.transformerSetLoader.takeIf { builder.autoInitialization },
        autoInitialization = builder.autoInitialization,
        dispatcher = builder.dispatcher,
    )
}

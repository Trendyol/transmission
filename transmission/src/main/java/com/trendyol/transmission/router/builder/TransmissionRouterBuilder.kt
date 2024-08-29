package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.request.Contract
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.identity

object TransmissionRouterBuilder {

    fun build(
        identity: Contract.Identity = Contracts.identity("router"),
        scope: TransmissionRouterBuilderScope.() -> Unit
    ): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            identity = identity,
            transformerSetLoader =
            builder.transformerSetLoader.takeIf { builder.autoInitialization },
            autoInitialization = builder.autoInitialization,
            dispatcher = builder.dispatcher,
        )
    }
}

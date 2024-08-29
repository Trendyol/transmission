package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter
import com.trendyol.transmission.transformer.request.Contracts
import com.trendyol.transmission.transformer.request.identity

object TransmissionTestingRouterBuilder {

    fun build(scope: TransmissionTestingRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            identity = Contracts.identity("testingRouter"),
            transformerSetLoader = builder.transformerSetLoader,
            dispatcher = builder.dispatcher,
            registryScope = builder.registryScope
        )
    }
}

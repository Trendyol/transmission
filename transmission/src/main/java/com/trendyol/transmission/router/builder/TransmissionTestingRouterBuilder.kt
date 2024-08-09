package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter

object TransmissionTestingRouterBuilder {

    fun build(scope: TransmissionTestingRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            transformerSetLoader = builder.transformerSetLoader,
            dispatcher = builder.dispatcher,
            registryScope = builder.registryScope
        )
    }
}

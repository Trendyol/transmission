package com.trendyol.transmission.router.builder

import com.trendyol.transmission.TransmissionRouter

object TransmissionTestingRouterBuilder {

    fun build(scope: TransmissionTestingRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            transformerSet = builder.transformerSet,
            dispatcher = builder.dispatcher,
            registryScope = builder.registryScope
        )
    }
}

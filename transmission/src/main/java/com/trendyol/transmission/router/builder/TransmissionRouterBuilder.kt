package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter

object TransmissionRouterBuilder {

    fun build(scope: TransmissionRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            transformerSetLoader =
            builder.transformerSetLoader.takeIf { builder.autoInitialization },
            autoInitialization = builder.autoInitialization,
            dispatcher = builder.dispatcher,
        )
    }
}

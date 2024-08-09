package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.TransmissionRouter

object TransmissionRouterBuilder {

    fun build(scope: TransmissionRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            transformerSetLoader = builder.transformerSetLoader,
            dispatcher = builder.dispatcher,
        )
    }
}

package com.trendyol.transmission.router.builder

import com.trendyol.transmission.TransmissionRouter

object TransmissionRouterBuilder {

    fun build(scope: TransmissionRouterBuilderScope.() -> Unit): TransmissionRouter {
        val builder = TransmissionRouterBuilderInternal(scope)
        return TransmissionRouter(
            transformerSet = builder.transformerSet,
            dispatcher = builder.dispatcher,
        )
    }
}

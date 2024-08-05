package com.trendyol.transmission.router.builder

import com.trendyol.transmission.router.RegistryScope

interface TransmissionTestingRouterBuilderScope : TransmissionRouterBuilderScope {
    fun testing(scope: RegistryScope.() -> Unit = {})
}

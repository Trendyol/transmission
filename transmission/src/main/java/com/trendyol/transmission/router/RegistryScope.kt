package com.trendyol.transmission.router

import com.trendyol.transmission.Transmission

interface RegistryScope {

    fun <D : Transmission.Data> addQueryData(
        data: D,
        key: String,
    )

    fun <D : Transmission.Data> addComputation(
        data: D,
        key: String,
    )
}

internal class RegistryScopeImpl : RegistryScope {

    val dataMap: MutableMap<String, Transmission.Data> = mutableMapOf()
    val computationMap: MutableMap<String, Transmission.Data> = mutableMapOf()

    override fun <D : Transmission.Data> addQueryData(data: D, key: String) {
        dataMap[key] = data
    }

    override fun <D : Transmission.Data> addComputation(data: D, key: String) {
        computationMap[key] = data
    }
}

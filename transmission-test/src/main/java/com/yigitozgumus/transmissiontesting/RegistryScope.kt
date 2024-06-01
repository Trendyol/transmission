package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

interface RegistryScope {
    fun <D : Transmission.Data> addQueryData(
        data: D,
        owner: KClass<out Transformer>? = null,
    )

    fun <D : Transmission.Data> addComputation(
        data: D,
        owner: KClass<out Transformer>? = null
    )
}

class RegistryScopeImpl : RegistryScope {

    val dataMap: MutableMap<String, Transmission.Data> = mutableMapOf()
    val computationMap: MutableMap<String, Transmission.Data> = mutableMapOf()

    override fun <D : Transmission.Data> addQueryData(
        data: D,
        owner: KClass<out Transformer>?
    ) {
        dataMap[owner?.java?.simpleName.orEmpty() + data::class.java.simpleName] = data
    }

    override fun <D : Transmission.Data> addComputation(
        data: D,
        owner: KClass<out Transformer>?
    ) {
        computationMap[owner?.java?.simpleName.orEmpty() + data::class.java.simpleName] = data
    }

}

package com.yigitozgumus.transmissiontesting

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

interface RegistryScope<D : Transmission.Data, E : Transmission.Effect, T: Transformer<D,E>> {
    fun  addQueryData(
        data: D,
        owner: KClass<T>? = null,
    )

    fun  addComputation(
        data: D,
        owner: KClass<T>? = null
    )
}

internal class RegistryScopeImpl<D: Transmission.Data, E: Transmission.Effect, T: Transformer<D,E>> : RegistryScope<D,E,T> {

    val dataMap: MutableMap<String, D> = mutableMapOf()
    val computationMap: MutableMap<String, D> = mutableMapOf()

    override fun addQueryData(
        data: D,
        owner: KClass<T>?
    ) {
        dataMap[owner?.java?.simpleName.orEmpty() + data::class.java.simpleName] = data
    }

    override fun addComputation(
        data: D,
        owner: KClass<T>?
    ) {
        computationMap[owner?.java?.simpleName.orEmpty() + data::class.java.simpleName] = data
    }

}
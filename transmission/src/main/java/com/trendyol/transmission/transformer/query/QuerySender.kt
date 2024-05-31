package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission
import com.trendyol.transmission.transformer.Transformer
import kotlin.reflect.KClass

interface QuerySender {

    /**
     * Sends a [Query.Data] to targeted [Transformer]. Returns the result if exists
     * @param type Type of the [Transmission.Data]
     * @param owner Type of the [Transformer]
     */
    suspend fun <D : Transmission.Data> queryData(
        type: KClass<D>,
        owner: KClass<out Transformer>? = null
    ): D?

    /**
     * Sends a [Query.Computation] to targeted [Transformer]. Returns the result if exists.
     * @param type Type of the [Transmission.Data]
     * @param owner Type of the [Transformer]
     * @param invalidate Invalidates the computation cache if exists
     */
    suspend fun <D : Transmission.Data, T : Transformer> queryComputation(
        type: KClass<D>,
        owner: KClass<out T>,
        invalidate: Boolean = false,
    ): D?
}

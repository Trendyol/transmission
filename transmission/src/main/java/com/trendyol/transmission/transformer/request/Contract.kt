package com.trendyol.transmission.transformer.request

import com.trendyol.transmission.Transmission

sealed interface Contract {

    class Identity internal constructor(internal val key: String) : Contract

    class DataHolder<T : Transmission.Data?> internal constructor(
        internal val key: String,
    ) : Contract

    class Computation<T : Any> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    class ComputationWithArgs<A : Any, T : Any> internal constructor(
        internal val key: String,
        internal val useCache: Boolean = false
    ) : Contract

    class Execution internal constructor(internal val key: String) : Contract

    class ExecutionWithArgs<A : Any> internal constructor(
        internal val key: String
    ) : Contract
}

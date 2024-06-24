package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

sealed interface Contract {

    abstract class Data<T: Transmission.Data?> : Contract {
        abstract val key: String
    }

    abstract class Computation<T : Transmission.Data> : Contract {
        abstract val key: String
        open val useCache: Boolean = false
    }

    abstract class ComputationWithArgs<A : Any, T : Transmission.Data> : Contract {
        abstract val key: String
        open val useCache: Boolean = false
    }
}

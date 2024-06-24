package com.trendyol.transmission.transformer.query

import com.trendyol.transmission.Transmission

sealed interface Contract {

    abstract class Data<T : Transmission.Data?> : Contract {
        abstract val key: String
    }

    abstract class Computation<T: Any>: Contract {
        abstract val key: String
        open val useCache: Boolean = false
    }

    abstract class ComputationWithArgs<A : Any, T : Any> : Contract {
        abstract val key: String
        open val useCache: Boolean = false
    }

    abstract class Execution : Contract {
        abstract val key: String
    }

    abstract class ExecutionWithArgs<A : Any> : Contract {
        abstract val key: String
    }

}

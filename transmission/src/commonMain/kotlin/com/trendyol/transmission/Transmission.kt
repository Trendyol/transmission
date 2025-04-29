package com.trendyol.transmission

sealed interface Transmission {
    /**
     * Transmission type that should come from UI. [TransmissionRouter] only processes [Signal]s.
     *
     * */
    interface Signal : Transmission

    /**
     * Transmission type that should be used as a Side Effect. They can be created from [Signal]s or
     * other [Effect]s
     */
    interface Effect : Transmission

    /**
     * Main representation of Data. Usually [Signal]s and [Effect]s are converted into [Data] after
     * processing inside [com.trendyol.transmission.transformer.Transformer]s.
     */
    interface Data : Transmission
}

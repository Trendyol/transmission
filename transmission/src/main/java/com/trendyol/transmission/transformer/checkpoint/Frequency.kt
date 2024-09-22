package com.trendyol.transmission.transformer.checkpoint


sealed interface Frequency {
    data object Once : Frequency
    data object Continous : Frequency
}

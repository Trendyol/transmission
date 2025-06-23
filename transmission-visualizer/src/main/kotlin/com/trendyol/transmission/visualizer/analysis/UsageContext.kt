package com.trendyol.transmission.visualizer.analysis

enum class UsageContext {
    CREATION,      // Where the signal/effect is created/instantiated
    PROCESSING,    // Where it's handled in transformers
    CONSUMPTION,   // Where data is consumed
    PUBLICATION,   // Where effects are published
    ROUTING        // Where signals are routed/processed
}

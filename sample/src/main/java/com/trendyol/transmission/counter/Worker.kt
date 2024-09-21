package com.trendyol.transmission.counter

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.HandlerRegistry
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.onSignal

class Worker(val id: String) : Transformer() {

    override val handlers: HandlerRegistry = handlers {
        onSignal<CounterSignal.Lookup> {
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}


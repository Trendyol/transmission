package com.trendyol.transmission.counter

import com.trendyol.transmission.transformer.Transformer
import com.trendyol.transmission.transformer.handler.Handlers
import com.trendyol.transmission.transformer.handler.handlers
import com.trendyol.transmission.transformer.handler.register

class Worker(val id: String) : Transformer() {

    override val handlers: Handlers = handlers {
        register<CounterSignal.Lookup> {
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}

# Handlers

!!! warning "Work in Progress"
    This documentation is a work in progress. Content may change or be incomplete.

**Handlers** in the Transmission library are responsible for processing incoming Transmissions within a Transformer. They define how a Transformer responds to different types of Signals and Effects.

## Creating Handlers

Handlers are defined by overriding the `handlers` property in a Transformer:

```kotlin
class MyTransformer : Transformer() {
    override val handlers: Handlers = createHandlers {
        // Register signal and effect handlers here
        onSignal<MySignal> { signal ->
            // Process signal
        }
        
        onEffect<MyEffect> { effect ->
            // Process effect
        }
    }
}
```

## Signal Handlers

Signal handlers process incoming Signals and are registered using the `onSignal` function:

```kotlin
onSignal<MySignal> { signal ->
    // Process the signal
    // You can access properties of the signal
    val value = signal.someProperty
    
    // Perform operations based on the signal
    // ...
    
    // Optionally publish effects or update data holders
    publish(MyEffect(value))
    myDataHolder.update { MyData(value) }
}
```

## Effect Handlers

Effect handlers process incoming Effects and are registered using the `onEffect` function:

```kotlin
onEffect<MyEffect> { effect ->
    // Process the effect
    // You can access properties of the effect
    val value = effect.someProperty
    
    // Perform operations based on the effect
    // ...
    
    // Optionally publish more effects or update data holders
    publish(AnotherEffect(value))
    myDataHolder.update { MyData(value) }
}
```

## Communication Scope

Within a handler, you have access to a `CommunicationScope` that provides methods for interacting with the Transmission network:

- `publish(effect)`: Send an Effect to other Transformers
- `send(data)`: Send Data to the TransmissionRouter (equivalent to updating a DataHolder)
- `compute(contract, ...)`: Execute a computation in another Transformer
- `execute(contract, ...)`: Execute an action in another Transformer
- `getData(contract)`: Get data from a DataHolder in another Transformer

## Extending Handlers

If you need to add additional handlers to a Transformer after initialization, you can use `extendHandlers`:

```kotlin
class MyTransformer : Transformer() {
    override val handlers: Handlers = createHandlers {
        // Base handlers
    }
    
    override val extendedHandlers: ExtendedHandlers = extendHandlers {
        // Additional handlers
    }
}
```

## Order of Execution

When a Signal or Effect is received by a Transformer, it is processed by the first matching handler. Handlers are matched based on the exact type of the Transmission, not including subtypes.

If multiple Transformers have handlers for the same type of Transmission, all of those handlers will be executed, but the order is not guaranteed.
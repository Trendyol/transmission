# Transformer

A **Transformer** is a core component in the Transmission library that processes transmissions. It acts as a processing unit within the communication network.

## Responsibilities

Transformers have the following responsibilities:

- Process incoming **Signals** and **Effects**
- Potentially produce new **Effects** or **Data**
- Maintain internal state if needed
- Expose data via DataHolders
- Provide computations and executions for other components to use

## Creating a Transformer

To create a Transformer, extend the `Transformer` class and override the `handlers` property:

```kotlin
class MyTransformer : Transformer() {
    override val handlers: Handlers = createHandlers {
        onSignal<MySignal> { signal ->
            // Process the signal
            // Optionally publish effects or data
            publish(MyEffect)
            
            // Update data holders
            myDataHolder.update { MyData("updated information") }
        }
        
        onEffect<MyEffect> { effect ->
            // Process the effect
            // Optionally publish more effects or data
        }
    }
}
```

## Handler Registration

Transformer uses a handler-based approach to process transmissions:

- `onSignal<T>` - Registers a handler for a specific Signal type
- `onEffect<T>` - Registers a handler for a specific Effect type

Within these handlers, you can:
- Process the incoming transmission
- Publish new Effects using `publish(effect)`
- Update DataHolders to produce Data

## DataHolders

Transformers can maintain state using DataHolders. Learn more in the [DataHolder](dataholder.md) section.

## Computations and Executions

Transformers can expose functionality to other components:

- **Computations**: Functions that return values
- **Executions**: Functions that perform actions without returning values

Learn more in the [Contracts](contracts.md) section.
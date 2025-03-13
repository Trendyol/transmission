# DataHolder

A **DataHolder** is a component in the Transmission library that allows Transformers to maintain and expose state. It acts as a container for data that can be updated and accessed by Transformers.

## Purpose

DataHolders serve several purposes:

- Maintain state within a Transformer
- Publish data changes to the TransmissionRouter
- Allow other Transformers to access the contained data via Contracts
- Provide a reactive way to observe data changes

## Creating a DataHolder

To create a DataHolder, use the `dataHolder` extension function on a Transformer:

```kotlin
class MyTransformer : Transformer() {
    private val myDataHolder = dataHolder<MyData?>(
        initialValue = null,
        contract = myContract,
        publishUpdates = true
    )
    
    // Rest of the implementation...
}
```

## Parameters

The `dataHolder` function takes the following parameters:

- `initialValue`: The initial value of the DataHolder (must be a subtype of `Transmission.Data` or null)
- `contract` (optional): When defined, data inside the holder can be accessed by other Transformers using `RequestHandler.query`
- `publishUpdates` (optional, default = true): Controls whether updates are sent to the TransmissionRouter

## Updating Data

To update the data in a DataHolder, use the `update` method:

```kotlin
myDataHolder.update { previousData ->
    // Create a new data instance based on previous data
    MyData("updated value based on $previousData")
}
```

The update function provides access to the previous value, allowing you to create a new value based on it.

## Accessing Data

You can access the current data in a DataHolder directly:

```kotlin
val currentData = myDataHolder.value
```

## Data Access from Other Transformers

To access data from a DataHolder in another Transformer, you need to use a Contract and the RequestHandler:

```kotlin
// Define a contract (usually in a shared location)
val myDataContract = Contracts.dataHolder<MyData>()

// In a transformer that needs to access the data
val data = requestHelper.getData(myDataContract)
```

See the [Contracts](contracts.md) section for more information on using Contracts for inter-transformer communication.
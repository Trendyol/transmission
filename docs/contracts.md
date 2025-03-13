# Contracts

**Contracts** in the Transmission library are a mechanism to enable communication between different Transformers. They provide a type-safe way to access data and functionality across the communication network.

## Types of Contracts

The Transmission library provides several types of contracts:

- **DataHolder Contracts**: Used to access data in a DataHolder
- **Computation Contracts**: Used to execute computations in a Transformer
- **Execution Contracts**: Used to execute actions in a Transformer
- **Identity Contracts**: Used to identify components in the system

## Creating Contracts

Contracts are created using the `Contracts` factory class:

```kotlin
// Create a DataHolder contract
val myDataContract = Contracts.dataHolder<MyData>()

// Create a Computation contract (no arguments)
val myComputationContract = Contracts.computation<Int>()

// Create a Computation contract (with arguments)
val myComputationWithArgsContract = Contracts.computationWithArgs<String, Boolean>()

// Create an Execution contract (no arguments)
val myExecutionContract = Contracts.execution()

// Create an Execution contract (with arguments)
val myExecutionWithArgsContract = Contracts.executionWithArgs<MyArgs>()

// Create an Identity contract
val myIdentityContract = Contracts.identity()
```

## Using Contracts

Contracts are used in different ways depending on their type:

### DataHolder Contracts

```kotlin
// In a Transformer that exposes data
private val dataHolder = dataHolder<MyData?>(
    initialValue = null,
    contract = myDataContract
)

// In a Transformer that needs to access the data
val data = requestHelper.getData(myDataContract)
```

### Computation Contracts

```kotlin
// In a Transformer that provides the computation
override val computations: Computations = createComputations {
    register(myComputationContract) {
        // Perform computation and return result
        42
    }
    
    register(myComputationWithArgsContract) { arg: String ->
        // Perform computation using the argument
        arg.isNotEmpty()
    }
}

// In a Transformer that needs to use the computation
val result = requestHelper.compute(myComputationContract)
val resultWithArg = requestHelper.compute(myComputationWithArgsContract, "test")
```

### Execution Contracts

```kotlin
// In a Transformer that provides the execution
override val executions: Executions = createExecutions {
    register(myExecutionContract) {
        // Perform an action
        println("Execution performed")
    }
    
    register(myExecutionWithArgsContract) { arg: MyArgs ->
        // Perform an action using the argument
        println("Execution with args: $arg")
    }
}

// In a Transformer that needs to trigger the execution
requestHelper.execute(myExecutionContract)
requestHelper.execute(myExecutionWithArgsContract, MyArgs("test"))
```

## Contract Keys

Each contract has a unique key that is used to identify it within the system. By default, contracts are given automatically generated unique keys, but you can specify your own keys:

```kotlin
val myDataContract = Contracts.dataHolder<MyData>("my_data_key")
```

Using explicit keys can be helpful for debugging and for ensuring that the contract is consistently identified across different parts of your application.
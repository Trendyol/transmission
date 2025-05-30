# Transformer

A **Transformer** is the core component that processes transmissions and contains your business logic. Transformers receive Signals and Effects, process them, and produce Data or additional Effects.

## Basic Structure

```kotlin
class MyTransformer : Transformer() {
    
    override val handlers: Handlers = handlers {
        // Define signal and effect handlers
    }
    
    override val computations: Computations = computations {
        // Define computations for inter-transformer communication
    }
    
    override val executions: Executions = executions {
        // Define executions for fire-and-forget operations
    }
}
```

## Constructor Parameters

```kotlin
open class Transformer(
    identity: Contract.Identity = Contract.identity(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val capacity: Capacity = Capacity.Default
)
```

- **identity**: Unique identifier for the transformer (auto-generated if not provided)
- **dispatcher**: Coroutine dispatcher for processing (defaults to `Dispatchers.Default`)
- **capacity**: Buffer capacity for internal channels

## Handlers

Handlers define how your Transformer responds to incoming Signals and Effects.

### Signal Handlers

```kotlin
override val handlers: Handlers = handlers {
    onSignal<UserLoginSignal> { signal ->
        // Process the login signal
        val result = authenticateUser(signal.credentials)
        
        if (result.isSuccess) {
            send(UserData(result.user))
            publish(NavigationEffect.GoToHome)
        } else {
            send(ErrorData("Login failed"))
        }
    }
}
```

### Effect Handlers

```kotlin
override val handlers: Handlers = handlers {
    onEffect<RefreshDataEffect> { effect ->
        // Handle refresh effect
        val freshData = fetchFreshData()
        send(DataRefreshedData(freshData))
    }
}
```

### Complete Example from Counter Sample

```kotlin
class Worker(val id: String) : Transformer() {
    
    override val handlers: Handlers = handlers {
        onSignal<CounterSignal.Lookup> {
            send(CounterData("Transformer $id updated data to ${compute(lookUpAndReturn, id)}"))
        }
    }
}
```

## Communication Scope

Within handler lambdas, you have access to `CommunicationScope` which provides:

### Sending Data

```kotlin
// Send data to the router's data stream
send(UserData(user))
```

### Publishing Effects

```kotlin
// Publish effect to other transformers
publish(LoggingEffect("User logged in"))

// Publish effect to specific transformer
publish(
    effect = NotificationEffect("Welcome!"),
    identity = notificationTransformerIdentity
)
```

### Query Other Transformers

```kotlin
// Compute value from another transformer
val userData = compute(userDataContract)

// Compute with arguments
val validationResult = compute(validationContract, inputData)
```

### Execute Operations

```kotlin
// Fire-and-forget execution
execute(cleanupContract)

// Execute with arguments
execute(logContract, "User action performed")
```

## Data Holders

Data Holders provide state management within Transformers:

```kotlin
class UserTransformer : Transformer() {
    
    // Create a data holder with initial state
    private val userHolder = dataHolder(
        initialValue = UserState(),
        contract = userStateContract
    )
    
    override val handlers: Handlers = handlers {
        onSignal<UpdateUserSignal> { signal ->
            // Update the held state
            userHolder.update { currentState ->
                currentState.copy(name = signal.newName)
            }
            
            // The updated state is automatically sent as Data
        }
        
        onSignal<GetUserSignal> {
            // Access current state
            val currentUser = userHolder.getValue()
            send(CurrentUserData(currentUser))
        }
    }
}
```

### Data Holder Features

- **Automatic Data Publishing**: Updates are automatically sent to the data stream
- **Thread-Safe**: Updates are synchronized
- **State Tracking**: Previous states are tracked for debugging

### Creating Data Holders

```kotlin
// Basic data holder
val holder = dataHolder(initialValue = MyState(), contract = myContract)

// Data holder without auto-publishing  
val holder = dataHolder(
    initialValue = MyState(), 
    contract = myContract,
    publishUpdates = false
)
```

## Computations

Computations enable inter-transformer communication for retrieving data:

```kotlin
class CalculatorTransformer : Transformer() {
    
    override val computations: Computations = computations {
        // Simple computation
        register(currentValueContract) {
            getCurrentCalculatorValue()
        }
        
        // Computation with arguments
        register(calculateContract) { input: CalculationInput ->
            performCalculation(input)
        }
        
        // Cached computation (result is cached)
        register(expensiveCalculationContract) {
            expensiveOperation()
        }
    }
}
```

### Using Computations

```kotlin
override val handlers: Handlers = handlers {
    onSignal<DisplayResultSignal> {
        // Query another transformer's computation
        val currentValue = compute(currentValueContract)
        send(DisplayData(currentValue))
    }
    
    onSignal<PerformCalculationSignal> { signal ->
        // Query with arguments
        val result = compute(calculateContract, signal.input)
        send(CalculationResultData(result))
    }
}
```

## Executions

Executions are fire-and-forget operations for side effects:

```kotlin
class LoggingTransformer : Transformer() {
    
    override val executions: Executions = executions {
        // Simple execution
        register(logMessageContract) {
            writeToLogFile("Message logged")
        }
        
        // Execution with arguments
        register(logWithLevelContract) { logEntry: LogEntry ->
            writeToLogFile(logEntry.level, logEntry.message)
        }
    }
}
```

### Using Executions

```kotlin
override val handlers: Handlers = handlers {
    onSignal<UserActionSignal> { signal ->
        // Fire-and-forget logging
        execute(logMessageContract)
        
        // With arguments
        execute(logWithLevelContract, LogEntry(LogLevel.INFO, "User action: ${signal.action}"))
        
        // Continue with main logic
        send(ActionCompletedData(signal.action))
    }
}
```

## Advanced Features

### Checkpoint Tracking (Experimental)

Checkpoints enable debugging and flow control:

```kotlin
@OptIn(ExperimentalTransmissionApi::class)
class ComplexTransformer : Transformer() {
    
    override val handlers: Handlers = handlers {
        onSignal<ProcessDataSignal> { signal ->
            // Pause execution at checkpoint
            val validatedData = pauseOn(validationCheckpoint)
            
            // Continue processing
            send(ProcessedData(validatedData))
        }
        
        onEffect<ValidationCompleteEffect> { effect ->
            // Validate checkpoint with result
            validate(validationCheckpoint, effect.validatedData)
        }
    }
    
    companion object {
        val validationCheckpoint = Contract.checkpointWithArgs<ValidatedData>()
    }
}
```

### Custom Error Handling

```kotlin
class SafeTransformer : Transformer() {
    
    override fun onError(throwable: Throwable) {
        // Handle errors that occur in this transformer
        publish(ErrorEffect("Transformer error: ${throwable.message}"))
    }
    
    override val handlers: Handlers = handlers {
        onSignal<RiskyOperationSignal> { signal ->
            try {
                val result = riskyOperation(signal.data)
                send(SuccessData(result))
            } catch (e: Exception) {
                send(ErrorData(e.message))
            }
        }
    }
}
```

### Lifecycle Management

```kotlin
class ResourceTransformer : Transformer() {
    
    private val database = openDatabase()
    
    override fun onCleared() {
        // Cleanup when transformer is cleared
        database.close()
        super.onCleared()
    }
}
```

## Complete Example from Components Sample

```kotlin
class InputTransformer(
    private val defaultDispatcher: CoroutineDispatcher
) : Transformer(dispatcher = defaultDispatcher) {

    private val holder = dataHolder(InputUiState(), holderContract)

    override val computations: Computations = computations {
        register(writtenInputContract) {
            delay(1.seconds)
            WrittenInput(holder.getValue().writtenText)
        }
        register(writtenInputWithArgs) {
            WrittenInput(it)
        }
    }

    @OptIn(ExperimentalTransmissionApi::class)
    override val handlers: Handlers = handlers {
        onSignal<InputSignal.InputUpdate> { signal ->
            holder.update { it.copy(writtenText = signal.value) }
            val color = pauseOn(colorCheckpoint)
            send(
                effect = ColorPickerEffect.SelectedColorUpdate(color),
                identity = multiOutputTransformerIdentity
            )
            publish(effect = InputEffect.InputUpdate(signal.value))
        }
        onEffect<ColorPickerEffect.BackgroundColorUpdate> { effect ->
            validate(colorCheckpoint, effect.color)
            holder.update { it.copy(backgroundColor = effect.color) }
        }
    }

    companion object {
        val writtenInputWithArgs = Contract.computationWithArgs<String, WrittenInput>()
        val writtenInputContract = Contract.computation<WrittenInput>()
        val holderContract = Contract.dataHolder<InputUiState>()
        val colorCheckpoint = Contract.checkpointWithArgs<Color>()
    }
}
```

## Best Practices

### 1. Single Responsibility
Keep each Transformer focused on a specific domain:

```kotlin
// Good - focused on user authentication
class AuthTransformer : Transformer()

// Good - focused on data caching  
class CacheTransformer : Transformer()

// Avoid - mixed responsibilities
class AuthAndCacheTransformer : Transformer()
```

### 2. Immutable State
Use immutable data classes for state:

```kotlin
data class UserState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
```

### 3. Error Handling
Handle errors gracefully:

```kotlin
onSignal<LoadDataSignal> { signal ->
    try {
        val data = loadData(signal.id)
        send(DataLoadedData(data))
    } catch (e: Exception) {
        send(ErrorData("Failed to load data: ${e.message}"))
    }
}
```

### 4. Use Contracts for Communication
Define contracts for inter-transformer communication:

```kotlin
companion object {
    val userDataContract = Contract.computation<UserData>()
    val validateUserContract = Contract.computationWithArgs<User, Boolean>()
}
```